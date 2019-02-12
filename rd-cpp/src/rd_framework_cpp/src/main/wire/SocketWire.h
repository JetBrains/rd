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
#include "ByteBufferAsyncProcessor.h"

#include <string>
#include <array>
#include <condition_variable>
#include <type_traits>

namespace rd {
	class SocketWire {
		static std::chrono::milliseconds timeout;
	public:
		class Base : public WireBase {
		protected:
			static Logger logger;

			std::timed_mutex lock;
			mutable std::mutex send_lock;
			mutable std::mutex socket_lock;

			std::thread thread;

			std::string id;
			Lifetime lifetime;
			IScheduler *scheduler = nullptr;
			std::shared_ptr<CSimpleSocket> socketProvider;

			std::shared_ptr<CActiveSocket> socket = std::make_shared<CActiveSocket>();

			mutable std::condition_variable send_var;
			mutable ByteBufferAsyncProcessor sendBuffer{id + "-AsyncSendProcessor",
														[this](Buffer::ByteArray it) { this->send0(std::move(it)); }};

			// mutable Buffer::ByteArray threadLocalSendByteArray;

			static const size_t RECIEVE_BUFFER_SIZE = 1u << 16;
			mutable std::array<Buffer::word_t, RECIEVE_BUFFER_SIZE> receiver_buffer;
			mutable decltype(receiver_buffer)::iterator lo = receiver_buffer.begin(), hi = receiver_buffer.begin();

			bool ReadFromSocket(Buffer::word_t *res, int32_t msglen) const;

		public:
			//region ctor/dtor

			Base(const std::string &id, Lifetime lifetime, IScheduler *scheduler);

			virtual ~Base() = default;
			//endregion

			void receiverProc() const;

			void send0(Buffer::ByteArray msg) const;

			void send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const override;

			void set_socket_provider(std::shared_ptr<CSimpleSocket> new_socket);
		};

		class Client : public Base {
		public:
			uint16_t port = 0;

			//region ctor/dtor

			Client(Lifetime lifetime, IScheduler *scheduler, uint16_t port, const std::string &id);

			virtual ~Client() = default;
			//endregion

			std::condition_variable_any cv;
		};

		class Server : public Base {
		public:
			uint16_t port = 0;

			std::unique_ptr<CPassiveSocket> ss = std::make_unique<CPassiveSocket>();

			//region ctor/dtor

			Server(Lifetime lifetime, IScheduler *scheduler, uint16_t port, const std::string &id);

			virtual ~Server() = default;
			//endregion
		};

	};
}


#endif //RD_CPP_SOCKETWIRE_H
