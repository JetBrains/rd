//
// Created by jetbrains on 23.08.2018.
//

#include "SocketWire.h"

#include <utility>
#include <thread>

rd::Logger SocketWire::Base::logger;

std::chrono::milliseconds SocketWire::timeout = std::chrono::milliseconds(500);

SocketWire::Base::Base(const std::string &id, Lifetime lifetime, IScheduler *scheduler)
        : WireBase(scheduler), id(id), lifetime(std::move(lifetime)),
          scheduler(scheduler)/*, threadLocalSendByteArray(16384)*/ {

}

void SocketWire::Base::receiverProc() const {
    while (!lifetime->is_terminated()) {
        try {
            if (!socketProvider->IsSocketValid()) {
                logger.debug(this->id + ": stop receive messages because socket disconnected");
//                sendBuffer.terminate();
                break;
            }

            int32_t sz = 0;
            MY_ASSERT_THROW_MSG(ReadFromSocket(reinterpret_cast<char *>(&sz), 4),
                                this->id + ": failed to read message size");
            logger.info(this->id + ": were received size and " + std::to_string(sz) + " bytes");
            Buffer msg(sz);
            MY_ASSERT_THROW_MSG(ReadFromSocket(reinterpret_cast<char *>(msg.data()), sz),
                                this->id + ": failed to read message");

            RdId id = RdId::read(msg);
            logger.debug(this->id + ": message received");
            message_broker.dispatch(id, std::move(msg));
            logger.debug(this->id + ": message dispatched");
        } catch (std::exception const &ex) {
            logger.error(this->id + " caught processing", &ex);
//            sendBuffer.terminate();
            break;
        }
    }
}

void SocketWire::Base::send0(const Buffer &msg) const {
    try {
        std::lock_guard<std::recursive_mutex> _(socket_lock);
        int32_t msglen = static_cast<int32_t>(msg.size());
        MY_ASSERT_THROW_MSG(socketProvider->Send(msg.data(), msglen) == msglen,
                            this->id + ": failed to send message over the network");
        logger.info(this->id + ": were sent " + std::to_string(msglen) + " bytes");
        /*MY_ASSERT_THROW_MSG(socketProvider->Send(msg.data(), 0) > 0,
                            this->id + ": failed to flush");*/
//        MY_ASSERT_MSG(socketProvider->Flush(), this->id + ": failed to flush");
    } catch (...) {
//        sendBuffer.terminate();
    }
}

void SocketWire::Base::send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const {
    MY_ASSERT_MSG(!id.isNull(), this->id + ": id mustn't be null");

    Buffer buffer{threadLocalSendByteArray};
    buffer.write_pod<int32_t>(0); //placeholder for length

    id.write(buffer); //write id
    writer(buffer); //write rest

    int32_t len = buffer.get_position();

    buffer.rewind();
    buffer.write_pod<int32_t>(len - 4);

    threadLocalSendByteArray = buffer.getArray();
    threadLocalSendByteArray.resize(len);
    /*sendBuffer.put(bytes);*/
    std::unique_lock<std::mutex> ul(send_lock);
    send_var.wait(ul, [this]() -> bool { return socketProvider != nullptr; });
    send0(Buffer(threadLocalSendByteArray));//todo not copy array
}

void SocketWire::Base::set_socket_provider(std::shared_ptr<CSimpleSocket> new_socket) {
    {
        std::unique_lock<std::mutex> ul(send_lock);
        socketProvider = std::move(new_socket);
        send_var.notify_all();
    }
    {
        std::lock_guard<std::timed_mutex> guard(lock);
        if (lifetime->is_terminated()) {
            return;
        }

//        sendBuffer.start();
    }
    receiverProc();
}

bool SocketWire::Base::ReadFromSocket(char *res, int32_t msglen) const {
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
            logger.info(this->id + ": receive finished: " + to_string(read) + "bytes read");
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
                    MY_ASSERT_THROW_MSG(socket->DisableNagleAlgoritm(), this->id + ": failed to DisableNagleAlgoritm");

                    // On windows connect will try to send SYN 3 times with interval of 500ms (total time is 1second)
                    // Connect timeout doesn't work if it's more than 1 second. But we don't need it because we can close socket any moment.

                    //https://stackoverflow.com/questions/22417228/prevent-tcp-socket-connection-retries
                    //HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\TcpMaxConnectRetransmissions
                    MY_ASSERT_THROW_MSG(socket->Open("127.0.0.1", this->port), this->id + ": failed to open ActiveSocket");

                    {
                        std::lock_guard<std::timed_mutex> guard(lock);
                        if (lifetime->is_terminated()) {
                            rd::catch_([this]() { socket->Close(); });
                        }
                    }

                    set_socket_provider(socket);
                } catch (std::exception const &e) {
                    std::lock_guard<std::timed_mutex> guard(lock);
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

//        bool sendBufferStopped = sendBuffer.stop(timeout);
//        logger.debug(this->id + ": send buffer stopped, success: " + std::to_string(sendBufferStopped));

        {
            std::lock_guard<std::timed_mutex> guard(lock);
            logger.debug(this->id + ": closing socket");
            rd::catch_([this]() {
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
    MY_ASSERT_MSG(ss->Listen("127.0.0.1", port), this->id + ": failed to listen socket on port:" + std::to_string(port));

    this->port = ss->GetServerPort();
    MY_ASSERT_MSG(this->port != 0, this->id + ": port wasn't chosen");

    thread = std::thread([this, lifetime]() mutable {
        try {
            socket.reset(ss->Accept());
            MY_ASSERT_THROW_MSG(socket != nullptr, this->id + ": accepting failed");
            logger.info(this->id + this->id + ": accepted passive socket");
            MY_ASSERT_THROW_MSG(socket->DisableNagleAlgoritm(), this->id + ": tcpNoDelay failed");

            {
                std::lock_guard<std::timed_mutex> guard(lock);
                if (lifetime->is_terminated()) {
                    rd::catch_([this]() {
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

//        bool sendBufferStopped = sendBuffer.stop(timeout);
//        logger.debug(this->id + ": send buffer stopped, success: " + std::to_string(sendBufferStopped));

        rd::catch_([this] {
            logger.debug(this->id + ": closing socket");
            MY_ASSERT_THROW_MSG(ss->Close(), this->id + ": failed to close socket");
        });
		rd::catch_([this] {
            {
                std::lock_guard<std::timed_mutex> guard(lock);
                logger.debug(this->id + ": closing socket");
                if (socket != nullptr) {
                    MY_ASSERT_THROW_MSG(socket->Close(), this->id + ": failed to close socket");
                }
            }
        });

        logger.debug(this->id + ": waiting for receiver thread");
        thread.join();
        logger.info(this->id + ": termination finished");
    });
}

