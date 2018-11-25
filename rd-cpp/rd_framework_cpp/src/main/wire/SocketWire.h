//
// Created by jetbrains on 23.08.2018.
//

#ifndef RD_CPP_SOCKETWIRE_H
#define RD_CPP_SOCKETWIRE_H

#include "IScheduler.h"
#include "WireBase.h"

#include "ActiveSocket.h"
#include "PassiveSocket.h"
#include "SimpleSocket.h"
#include "Logger.h"
#include "threading/ByteBufferAsyncProcessor.h"

#include <string>
#include <array>
#include <condition_variable>


class Lifetime;

class SocketWire {
    static std::chrono::milliseconds timeout;
public:
    class Base : public WireBase {
    protected:
        Logger logger;

        std::timed_mutex lock;
        mutable std::mutex send_lock;
        mutable std::recursive_mutex socket_lock;

        std::thread thread;

        std::string id;
        Lifetime lifetime;
        IScheduler *scheduler = nullptr;
        std::shared_ptr<CSimpleSocket> socketProvider;

        std::shared_ptr<CActiveSocket> socket = std::make_shared<CActiveSocket>();

        mutable std::condition_variable send_var;
        /*mutable ByteBufferAsyncProcessor sendBuffer{id + "-AsyncSendProcessor",
                                                    [this](ByteArraySlice const &it) { this->send0(it); }};*/

        mutable Buffer::ByteArray threadLocalSendByteArray;


        mutable std::array<Buffer::word_t, 1u << 16> receiver_buffer;
        mutable decltype(receiver_buffer)::iterator lo = receiver_buffer.begin(), hi = receiver_buffer.begin();

        bool ReadFromSocket(char *res, int32_t msglen) const;

    public:
        //region ctor/dtor

        Base(const std::string &id, Lifetime lifetime, IScheduler *scheduler);

        virtual ~Base() = default;
        //endregion

        void receiverProc() const;

        void send0(const Buffer &msg) const;

        void send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const override;

        void set_socket_provider(std::shared_ptr<CSimpleSocket> new_socket);
    };

    class Client : public Base {
    public:
        uint16_t port = 0;

        //region ctor/dtor

        virtual ~Client() = default;
        //endregion

        std::condition_variable_any cv;

        Client(Lifetime lifetime, IScheduler *scheduler, uint16_t port, const std::string &id);
    };

    class Server : public Base {
    public:
        uint16_t port = 0;

        std::unique_ptr<CPassiveSocket> ss = std::make_unique<CPassiveSocket>();

        //region ctor/dtor

        virtual ~Server() = default;
        //endregion

        Server(Lifetime lifetime, IScheduler *scheduler, uint16_t port, const std::string &id);
    };

};


#endif //RD_CPP_SOCKETWIRE_H
