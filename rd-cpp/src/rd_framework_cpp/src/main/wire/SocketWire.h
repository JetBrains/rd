#ifndef RD_CPP_SOCKETWIRE_H
#define RD_CPP_SOCKETWIRE_H

#include "IScheduler.h"
#include "WireBase.h"
#include "Logger.h"
#include "ByteBufferAsyncProcessor.h"

#include "SimpleSocket.h"
#include "ActiveSocket.h"
#include "PassiveSocket.h"

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
			mutable std::mutex socket_send_lock;

			std::thread thread{};

			std::string id;
			Lifetime lifetime;
			IScheduler *scheduler = nullptr;
			std::shared_ptr<CSimpleSocket> socket_provider;

			std::shared_ptr<CActiveSocket> socket = std::make_shared<CActiveSocket>();

			mutable std::condition_variable send_var;
			mutable ByteBufferAsyncProcessor async_send_buffer{id + "-AsyncSendProcessor",
															   [this](Buffer::ByteArray it) {
																   this->send0(std::move(it));
															   }};

			// mutable Buffer::ByteArray threadLocalSendByteArray;

			static constexpr size_t RECIEVE_BUFFER_SIZE = 1u << 16;
			mutable std::array<Buffer::word_t, RECIEVE_BUFFER_SIZE> receiver_buffer;
			mutable decltype(receiver_buffer)::iterator lo = receiver_buffer.begin(), hi = receiver_buffer.begin();

			static constexpr size_t SEND_BUFFER_SIZE = 16 * 1024;
			mutable Buffer local_send_buffer;

			static constexpr int32_t ACK_MESSAGE_LENGTH = -1;
			static constexpr int32_t PACKAGE_HEADER_LENGTH = sizeof(ACK_MESSAGE_LENGTH) + sizeof(sequence_number_t);
			Buffer ack_buffer{PACKAGE_HEADER_LENGTH};

			mutable sequence_number_t max_received_seqn;
			mutable sequence_number_t max_sent_seqn;
			Buffer send_package_header{PACKAGE_HEADER_LENGTH};

			bool read_from_socket(Buffer::word_t *res, int32_t msglen) const;

			template<typename T>
			bool read_integral_from_socket(T &x) const {
				return read_from_socket(reinterpret_cast<Buffer::word_t *>(&x), sizeof(T));
			}

			bool read_data_from_socket(Buffer::word_t *data, size_t len) const {
				return read_from_socket(reinterpret_cast<Buffer::word_t *>(data), len);
			}
		public:

			//region ctor/dtor

			Base(std::string id, Lifetime lifetime, IScheduler *scheduler);
			virtual ~Base() = default;

			//endregion

			int32_t read_message_size() const;

			bool read_and_dispatch() const;

			void receiverProc() const;

			void send0(Buffer::ByteArray msg) const;

			void send(RdId const &rd_id, std::function<void(Buffer const &buffer)> writer) const override;

			void set_socket_provider(std::shared_ptr<CSimpleSocket> new_socket);

			CSimpleSocket *get_socket_provider() const;

			void send_ack(sequence_number_t seqn) const;
		};

		class Client : public Base {
		public:
			uint16_t port = 0;

			//region ctor/dtor

			Client(Lifetime lifetime, IScheduler *scheduler, uint16_t port = 0, const std::string &id = "ClientSocket");

			virtual ~Client() = default;
			//endregion

			std::condition_variable_any cv;
		};

		class Server : public Base {
		public:
			uint16_t port = 0;

			std::unique_ptr<CPassiveSocket> ss = std::make_unique<CPassiveSocket>();

			//region ctor/dtor

			Server(Lifetime lifetime, IScheduler *scheduler, uint16_t port = 0, const std::string &id = "ServerSocket");

			virtual ~Server() = default;
			//endregion
		};

	};
}


#endif //RD_CPP_SOCKETWIRE_H
