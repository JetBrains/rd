#include "SocketWire.h"

#include <utility>
#include <thread>

namespace rd {
	Logger SocketWire::Base::logger;

	std::chrono::milliseconds SocketWire::timeout = std::chrono::milliseconds(500);

	SocketWire::Base::Base(const std::string &id, Lifetime lifetime, IScheduler *scheduler)
			: WireBase(scheduler), id(id), lifetime(lifetime),
			  scheduler(scheduler), local_send_buffer(SEND_BUFFER_SIZE) {

	}

	void SocketWire::Base::receiverProc() const {
		while (!lifetime->is_terminated()) {
			try {
				if (!socketProvider->IsSocketValid()) {
					logger.debug(this->id + ": stop receive messages because socket disconnected");
					async_send_buffer.terminate();
					break;
				}

				int32_t sz = 0;
				MY_ASSERT_THROW_MSG(ReadFromSocket(reinterpret_cast<Buffer::word_t *>(&sz), 4),
									this->id + ": failed to read message size");
				logger.info(this->id + ": were received size and " + std::to_string(sz) + " bytes");
				Buffer msg(static_cast<size_t>(sz));
				MY_ASSERT_THROW_MSG(ReadFromSocket(reinterpret_cast<Buffer::word_t *>(msg.data()), sz),
									this->id + ": failed to read message");

				RdId id = RdId::read(msg);
				logger.debug(this->id + ": message received");
				message_broker.dispatch(id, std::move(msg));
				logger.debug(this->id + ": message dispatched");
			} catch (std::exception const &ex) {
				logger.error(this->id + " caught processing", &ex);
				async_send_buffer.terminate();
				break;
			}
		}
	}

	void SocketWire::Base::send0(Buffer::ByteArray msg) const {
		try {
			std::lock_guard<decltype(socket_lock)> guard(socket_lock);
			int32_t msglen = static_cast<int32_t>(msg.size());
			MY_ASSERT_THROW_MSG(socketProvider->Send(msg.data(), msglen) == msglen,
								this->id + ": failed to send message over the network");
			logger.info(this->id + ": were sent " + std::to_string(msglen) + " bytes");
			//        MY_ASSERT_MSG(socketProvider->Flush(), this->id + ": failed to flush");
		} catch (...) {
			async_send_buffer.terminate();
		}
	}

	void SocketWire::Base::send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const {
		MY_ASSERT_MSG(!id.isNull(), this->id + ": id mustn't be null");

		
		local_send_buffer.write_integral<int32_t>(0); //placeholder for length

		id.write(local_send_buffer); //write id
		writer(local_send_buffer); //write rest

		int32_t len = static_cast<int32_t>(local_send_buffer.get_position());

		int32_t position = static_cast<int32_t>(local_send_buffer.get_position());
		local_send_buffer.rewind();
		local_send_buffer.write_integral<int32_t>(len - 4);
		local_send_buffer.set_position(static_cast<size_t>(position));
		async_send_buffer.put(std::move(local_send_buffer).getRealArray());
		local_send_buffer.rewind();
	}

	void SocketWire::Base::set_socket_provider(std::shared_ptr<CSimpleSocket> new_socket) {
		{
			std::unique_lock<decltype(send_lock)> ul(send_lock);
			socketProvider = std::move(new_socket);
			send_var.notify_all();
		}
		{
			std::lock_guard<decltype(lock)> guard(lock);
			if (lifetime->is_terminated()) {
				return;
			}

			async_send_buffer.start();
		}
		receiverProc();
	}

	bool SocketWire::Base::ReadFromSocket(Buffer::word_t *res, int32_t msglen) const {
		int32_t ptr = 0;
		while (ptr < msglen) {
			MY_ASSERT_MSG(hi >= lo, "hi >= lo");

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
				if (!socketProvider->IsSocketValid()) {
					return false;
				}
				int32_t read = socketProvider->Receive(static_cast<int32_t>(receiver_buffer.end() - hi), &*hi);
				if (read == -1) {
					logger.error(this->id + ": socket was shutted down for receiving");
					return false;
				}
				hi += read;
				if (read > 0) {
					logger.info(this->id + ": receive finished: " + std::to_string(read) + "bytes read");
				}
			}
		}
		MY_ASSERT_MSG(ptr == msglen, "resPtr == res.Length");
		return true;
	}


	SocketWire::Client::Client(Lifetime lifetime, IScheduler *scheduler, uint16_t port = 0,
							   const std::string &id = "ClientSocket") : Base(id, lifetime, scheduler), port(port) {
		thread = std::thread([this, lifetime]() mutable {
			try {
				while (!lifetime->is_terminated()) {
					try {
						socket = std::make_shared<CActiveSocket>();
						MY_ASSERT_THROW_MSG(socket->Initialize(), this->id + ": failed to init ActiveSocket");
						MY_ASSERT_THROW_MSG(socket->DisableNagleAlgoritm(),
											this->id + ": failed to DisableNagleAlgoritm");

						// On windows connect will try to send SYN 3 times with interval of 500ms (total time is 1second)
						// Connect timeout doesn't work if it's more than 1 second. But we don't need it because we can close socket any moment.

						//https://stackoverflow.com/questions/22417228/prevent-tcp-socket-connection-retries
						//HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\TcpMaxConnectRetransmissions
						MY_ASSERT_THROW_MSG(socket->Open("127.0.0.1", this->port),
											this->id + ": failed to open ActiveSocket");

						{
							std::lock_guard<decltype(lock)> guard(lock);
							if (lifetime->is_terminated()) {
								catch_([this]() { socket->Close(); });
							}
						}

						set_socket_provider(socket);
					} catch (std::exception const &e) {
						std::lock_guard<decltype(lock)> guard(lock);
						bool shouldReconnect = false;
						if (!lifetime->is_terminated()) {
							cv.wait_for(lock, timeout);
							shouldReconnect = !lifetime->is_terminated();
						}
						if (shouldReconnect) {
							continue;
						}
					}
					break;
				}

			} catch (std::exception const &e) {
				logger.info(this->id + ": closed with exception: ", &e);
			}
			logger.debug(this->id + ": thread expired");
		});

		lifetime->add_action([this]() {
			logger.info(this->id + ": start terminating lifetime");

			bool send_buffer_stopped = async_send_buffer.stop(timeout);
			logger.debug(this->id + ": send buffer stopped, success: " + std::to_string(send_buffer_stopped));

			{
				std::lock_guard<decltype(lock)> guard(lock);
				logger.debug(this->id + ": closing socket");
				catch_([this]() {
					if (socket != nullptr) {
						MY_ASSERT_THROW_MSG(socket->Close(), this->id + ": failed to close socket");
					}
				});
				cv.notify_all();
			}

			logger.debug(this->id + ": waiting for receiver thread");
			thread.join();
			logger.info(this->id + ": termination finished");
		});
	}

	SocketWire::Server::Server(Lifetime lifetime, IScheduler *scheduler, uint16_t port = 0,
							   const std::string &id = "ServerSocket") : Base(id, lifetime, scheduler) {
		MY_ASSERT_MSG(ss->Initialize(), this->id + ": failed to initialize socket");
		MY_ASSERT_MSG(ss->Listen("127.0.0.1", port),
					  this->id + ": failed to listen socket on port:" + std::to_string(port));

		this->port = ss->GetServerPort();
		MY_ASSERT_MSG(this->port != 0, this->id + ": port wasn't chosen");

		thread = std::thread([this, lifetime]() mutable {
			try {
				logger.info(this->id + ": accepting started");
				CActiveSocket *accepted = ss->Accept();
				MY_ASSERT_THROW_MSG(accepted != nullptr, std::string(ss->DescribeError()))
				socket.reset(accepted);
				logger.info(this->id + ": accepted passive socket");
				MY_ASSERT_THROW_MSG(socket->DisableNagleAlgoritm(), this->id + ": tcpNoDelay failed")

				{
					std::lock_guard<decltype(lock)> guard(lock);
					if (lifetime->is_terminated()) {
						catch_([this]() {
							logger.debug(this->id + ": closing passive socket");
							MY_ASSERT_THROW_MSG(socket->Close(), this->id + ": failed to close socket");
							logger.info(this->id + ": close passive socket");
						});
					}
				}

				logger.debug(this->id + ": setting socket provider");
				set_socket_provider(socket);
			} catch (std::exception const &e) {
				logger.info(this->id + ": closed with exception: ", &e);
			}
			logger.debug(this->id + ": thread expired");
		});

		lifetime->add_action([this] {
			logger.info(this->id + ": start terminating lifetime");

			bool send_buffer_stopped = async_send_buffer.stop(timeout);
			logger.debug(this->id + ": send buffer stopped, success: " + std::to_string(send_buffer_stopped));

			catch_([this] {
				logger.debug(this->id + ": closing socket");
				MY_ASSERT_THROW_MSG(ss->Close(), this->id + ": failed to close socket");
			});
			catch_([this] {
				{
					std::lock_guard<decltype(lock)> guard(lock);
					logger.debug(this->id + ": closing socket");
					if (socket != nullptr) {
						MY_ASSERT_THROW_MSG(socket->Close(), this->id + ": failed to close socket");
					}
				}
			});

			logger.debug(this->id + ": waiting for receiver thread");
			logger.debug(this->id + ": is thread joinable? " + std::to_string(thread.joinable()));
			thread.join();
			logger.info(this->id + ": termination finished");
		});
	}
}

