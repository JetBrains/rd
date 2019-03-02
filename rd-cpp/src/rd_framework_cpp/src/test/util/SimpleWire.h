//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_TESTWIRE_H
#define RD_CPP_TESTWIRE_H


#include "WireBase.h"
#include "RdId.h"
#include "Buffer.h"

#include <queue>
#include <utility>

namespace rd {
	namespace test {
		class RdMessage {
		public:
			RdId id;
			Buffer buffer;

			RdMessage(const RdId &id, Buffer buffer) : id(id), buffer(std::move(buffer)) {};
		};

		class SimpleWire : public WireBase {
		protected:
			bool auto_flush = true;
		public:
			mutable SimpleWire const *counterpart = nullptr;
			mutable std::queue<RdMessage> msgQ;
			mutable int64_t bytesWritten = 0;

			//region ctor/dtor

			explicit SimpleWire(IScheduler *scheduler);

			virtual ~SimpleWire() = default;
			//endregion

			void send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const override;

			void process_all_messages() const;

			void process_one_message() const;

			void set_auto_flush(bool value);
		};
	}
}


#endif //RD_CPP_TESTWIRE_H
