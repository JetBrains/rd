//
// Created by jetbrains on 25.07.2018.
//

#include "SimpleWire.h"

namespace rd {
	namespace test {
		SimpleWire::SimpleWire(IScheduler *scheduler) : WireBase(scheduler) {
			this->connected.set(true);
		}

		void SimpleWire::send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const {
			assert(!id.isNull());
			Buffer ostream(10);
			writer(ostream);

			bytesWritten += ostream.get_position();

			ostream.rewind();

			msgQ.push(RdMessage(id, std::move(ostream)));
			if (auto_flush) {
				process_all_messages();
			}
		}

		void SimpleWire::process_all_messages() const {
			while (!msgQ.empty()) {
				process_one_message();
			}
		}

		void SimpleWire::process_one_message() const {
			if (msgQ.empty()) {
				return;
			}
			auto msg = std::move(msgQ.front());
			msgQ.pop();
			counterpart->message_broker.dispatch(msg.id, std::move(msg.buffer));
		}

		void SimpleWire::set_auto_flush(bool value) {
			auto_flush = value;
			if (value) {
				process_all_messages();
			}
		}
	}
}
