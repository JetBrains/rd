#include <utility>

#include "SocketWire.h"

#include <utility>
#include <thread>

namespace rd {
	Logger SocketWire::Base::logger;

	std::chrono::milliseconds SocketWire::timeout = std::chrono::milliseconds(500);

	constexpr int32_t SocketWire::Base::ACK_MESSAGE_LENGTH;
	constexpr int32_t SocketWire::Base::PACKAGE_HEADER_LENGTH;

	SocketWire::Base::Base(std::string id, Lifetime lifetime, IScheduler *scheduler)
			: WireBase(scheduler), id(std::move(id)), lifetime(lifetime),
			  scheduler(scheduler), local_send_buffer(SEND_BUFFER_SIZE) {
		async_send_buffer.pause("initial");
		async_send_buffer.start();
	}

	void SocketWire::Base::receiverProc() const {
		while (!lifetime->is_terminated()) {
			try {
				if (!socket_provider->IsSocketValid()) {
					logger.debug(this->id + ": stop receive messages because socket disconnected");
//					async_send_buffer.terminate();
					break;
				}

				if (!read_and_dispatch()) {
					logger.debug(id + ": connection was gracefully shutdown");
//					async_send_buffer.terminate();
					break;
				}
			} catch (std::exception const &ex) {
				logger.error(&ex, this->id + " caught processing");
//				async_send_buffer.terminate();
				break;
			}
		}
	}

	bool SocketWire::Base::send0(const Buffer::ByteArray &msg) const {
		try {
			std::lock_guard<decltype(socket_send_lock)> guard(socket_send_lock);

			int32_t msglen = static_cast<int32_t>(msg.size());

			send_package_header.rewind();
			send_package_header.write_integral(msglen);
			send_package_header.write_integral(++max_sent_seqn);

			RD_ASSERT_THROW_MSG(socket_provider->Send(send_package_header.data(), send_package_header.get_position()) ==
								PACKAGE_HEADER_LENGTH,
								this->id + ": failed to send header over the network")

			RD_ASSERT_THROW_MSG(socket_provider->Send(msg.data(), msglen) == msglen,
								this->id + ": failed to send package over the network");
			logger.info(this->id + ": were sent " + std::to_string(msglen) + " bytes");
			//        RD_ASSERT_MSG(socketProvider->Flush(), this->id + ": failed to flush");
			return true;
		} catch (std::exception const &e) {
			async_send_buffer.pause("send0");
			return false;
		}
	}

	void SocketWire::Base::send(RdId const &rd_id, std::function<void(Buffer const &buffer)> writer) const {
		RD_ASSERT_MSG(!rd_id.isNull(), this->id + ": id mustn't be null");


		local_send_buffer.write_integral<int32_t>(0); //placeholder for length

		rd_id.write(local_send_buffer); //write id
		writer(local_send_buffer); //write rest

		int32_t len = static_cast<int32_t>(local_send_buffer.get_position());

		local_send_buffer.rewind();
		local_send_buffer.write_integral<int32_t>(len - 4);
		local_send_buffer.set_position(static_cast<size_t>(len));
		async_send_buffer.put(std::move(local_send_buffer).getRealArray());
		local_send_buffer.rewind();
	}

	void SocketWire::Base::set_socket_provider(std::shared_ptr<CActiveSocket> new_socket) {
		{
			std::lock_guard<decltype(socket_send_lock)> guard(socket_send_lock);
			socket_provider = std::move(new_socket);
			socket_send_var.notify_all();
		}
		{
			std::lock_guard<decltype(lock)> guard(lock);
			if (lifetime->is_terminated()) {
				return;
			}
		}
		async_send_buffer.resume();

		receiverProc();

		connected.set(false);

		async_send_buffer.pause("Disconnected");
		if (!socket_provider->Close()) {
			//double close?
			logger.warn(this->id + ": possibly double close after disconnect");
		}
	}

	bool SocketWire::Base::read_from_socket(Buffer::word_t *res, int32_t msglen) const {
		int32_t ptr = 0;
		while (ptr < msglen) {
			RD_ASSERT_MSG(hi >= lo, "hi >= lo")

			int32_t rest = msglen - ptr;
			int32_t available = static_cast<int32_t>(hi - lo);

			if (available > 0) {
				int32_t copylen = (std::min)(rest, available);
				std::copy(lo, lo + copylen, res + ptr);
				lo += copylen;
				ptr += copylen;
			} else {
				if (hi == receiver_buffer.end()) {
					hi = lo = receiver_buffer.begin();
				}
				logger.info(this->id + ": receive started");
				if (!socket_provider->IsSocketValid()) {
					return false;
				}
				int32_t read = socket_provider->Receive(static_cast<int32_t>(receiver_buffer.end() - hi), &*hi);
				if (read == -1) {
					logger.error(this->id + ": error has occurred while receiving");
					return false;
				}
				if (read == 0) {
					logger.warn(this->id + ": socket was shutted down for receiving");
					return false;
				}
				hi += read;
				if (read > 0) {
					logger.info(this->id + ": receive finished: " + std::to_string(read) + "bytes read");
				}
			}
		}
		RD_ASSERT_MSG(ptr == msglen, "read invalid number of bytes from socket,"s
									 + "expected: "
									 + std::to_string(msglen)
									 + "actual: "
									 + std::to_string(ptr))
		return true;
	}

	int32_t SocketWire::Base::read_message_size() const {
		int32_t len = 0;
		sequence_number_t seqn = 0;
		while (true) {
			if (!read_integral_from_socket(len)) {
				return -1;
			}
			if (!read_integral_from_socket(seqn)) {
				return -1;
			}

			if (len == ACK_MESSAGE_LENGTH) {
				async_send_buffer.acknowledge(seqn);
			} else {
				if (seqn > max_received_seqn) {
					send_ack(seqn);
					max_received_seqn = seqn;
				}
				read_integral_from_socket<int32_t>(len);
				return len;
			}
		}
	}

	bool SocketWire::Base::read_and_dispatch() const {
		int32_t sz = read_message_size();
		if (sz == -1) {
			logger.debug(this->id + ": failed to read message size");
			return false;
		}
		logger.info(this->id + ": were received size and " + std::to_string(sz) + " bytes");
		Buffer msg(static_cast<size_t>(sz));
		if (!read_data_from_socket(msg.data(), sz)) {
			logger.debug(this->id + ": failed to read message");
			return false;
		}

		RdId rd_id = RdId::read(msg);
		logger.debug(this->id + ": message received");
		message_broker.dispatch(rd_id, std::move(msg));
		logger.debug(this->id + ": message dispatched");
		return true;
	}

	CSimpleSocket *SocketWire::Base::get_socket_provider() const {
		return socket_provider.get();
	}

	void SocketWire::Base::send_ack(sequence_number_t seqn) const {
		logger.trace(id + " send ack " + std::to_string(seqn));
		try {
			ack_buffer.rewind();

			ack_buffer.write_integral(ACK_MESSAGE_LENGTH);
			ack_buffer.write_integral(seqn);
			{
				std::lock_guard<decltype(socket_send_lock)> guard(socket_send_lock);
				RD_ASSERT_THROW_MSG(
						socket_provider->Send(ack_buffer.data(), ack_buffer.get_position()) == PACKAGE_HEADER_LENGTH,
						this->id + ": failed to send ack over the network")
			}
		} catch (std::exception const &e) {
			logger.warn(&e, id + ": exception raised during ACK, seqn = %d", seqn);
		}
	}


	SocketWire::Client::Client(Lifetime lifetime, IScheduler *scheduler, uint16_t port,
							   const std::string &id) : Base(id, lifetime, scheduler), port(port) {
		thread = std::thread([this, lifetime]() mutable {
			try {
				while (!lifetime->is_terminated()) {
					try {
						socket = std::make_shared<CActiveSocket>();
						RD_ASSERT_THROW_MSG(socket->Initialize(), this->id + ": failed to init ActiveSocket");
						RD_ASSERT_THROW_MSG(socket->DisableNagleAlgoritm(),
											this->id + ": failed to DisableNagleAlgoritm");

						// On windows connect will try to send SYN 3 times with interval of 500ms (total time is 1second)
						// Connect timeout doesn't work if it's more than 1 second. But we don't need it because we can close socket any moment.

						//https://stackoverflow.com/questions/22417228/prevent-tcp-socket-connection-retries
						//HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\TcpMaxConnectRetransmissions
						RD_ASSERT_THROW_MSG(socket->Open("127.0.0.1", this->port),
											this->id + ": failed to open ActiveSocket");

						{
							std::lock_guard<decltype(lock)> guard(lock);
							if (lifetime->is_terminated()) {
								if (!socket->Close()) {
									logger.error(this->id + "faile to close socket");
								}
								return;
							}
						}

						set_socket_provider(socket);
					} catch (std::exception const &e) {
						std::lock_guard<decltype(lock)> guard(lock);
						bool should_reconnect = false;
						if (!lifetime->is_terminated()) {
							cv.wait_for(lock, timeout);
							should_reconnect = !lifetime->is_terminated();
						}
						if (should_reconnect) {
							continue;
						}
						break;
					}
				}

			} catch (std::exception const &e) {
				logger.info(this->id + ": closed with exception: ", &e);
			}
			logger.debug(this->id + ": thread expired");
		});

		lifetime->add_action([this]() {
			logger.info(this->id + ": starts terminating lifetime");

			bool send_buffer_stopped = async_send_buffer.stop(timeout);
			logger.debug(this->id + ": send buffer stopped, success: " + std::to_string(send_buffer_stopped));

			{
				std::lock_guard<decltype(lock)> guard(lock);
				logger.debug(this->id + ": closing socket");

				if (socket != nullptr) {
					if (!socket->Close()) {
						logger.error(this->id + ": failed to close socket");
					}
				}
				cv.notify_all();
			}

			logger.debug(this->id + ": waiting for receiver thread");
			logger.debug(this->id + ": is thread joinable? " + std::to_string(thread.joinable()));
			thread.join();
			logger.info(this->id + ": termination finished");
		});
	}

	SocketWire::Server::Server(Lifetime lifetime, IScheduler *scheduler, uint16_t port,
							   const std::string &id) : Base(id, lifetime, scheduler) {
		RD_ASSERT_MSG(ss->Initialize(), this->id + ": failed to initialize socket");
		RD_ASSERT_MSG(ss->Listen("127.0.0.1", port),
					  this->id + ": failed to listen socket on port:" + std::to_string(port));

		this->port = ss->GetServerPort();
		RD_ASSERT_MSG(this->port != 0, this->id + ": port wasn't chosen")

		thread = std::thread([this, lifetime]() mutable {
			while (!lifetime->is_terminated()) {
				try {
					logger.info(this->id + ": accepting started");
					CActiveSocket *accepted = ss->Accept();
					RD_ASSERT_THROW_MSG(accepted != nullptr, std::string(ss->DescribeError()))
					socket.reset(accepted);
					logger.info(this->id + ": accepted passive socket");
					RD_ASSERT_THROW_MSG(socket->DisableNagleAlgoritm(), this->id + ": tcpNoDelay failed")

					{
						std::lock_guard<decltype(lock)> guard(lock);
						if (lifetime->is_terminated()) {
							logger.debug(this->id + ": closing passive socket");
							if (!socket->Close()) {
								logger.error(this->id + ": failed to close socket");
							}
							logger.info(this->id + ": close passive socket");
						}
					}

					logger.debug(this->id + ": setting socket provider");
					set_socket_provider(socket);
				} catch (std::exception const &e) {
					logger.info(this->id + ": closed with exception: ", &e);
				}
			}
			logger.debug(this->id + ": thread expired");
		});

		lifetime->add_action([this] {
			logger.info(this->id + ": start terminating lifetime");

			bool send_buffer_stopped = async_send_buffer.stop(timeout);
			logger.debug(this->id + ": send buffer stopped, success: " + std::to_string(send_buffer_stopped));


			logger.debug(this->id + ": closing server socket");
			if (!ss->Close()) {
				logger.error(this->id + ": failed to close server socket");
			}

			{
				std::lock_guard<decltype(lock)> guard(lock);
				logger.debug(this->id + ": closing socket");
				if (socket != nullptr) {
					if (!socket->Close()) {
						logger.error(this->id + ": failed to close socket");
					}
				}
			}

			logger.debug(this->id + ": waiting for receiver thread");
			logger.debug(this->id + ": is thread joinable? " + std::to_string(thread.joinable()));
			thread.join();
			logger.info(this->id + ": termination finished");
		});
	}
}

