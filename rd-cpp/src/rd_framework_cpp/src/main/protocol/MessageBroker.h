#ifndef RD_CPP_MESSAGEBROKER_H
#define RD_CPP_MESSAGEBROKER_H

#include "base/IRdReactive.h"
#include "logger/Logger.h"

#include "std/unordered_map.h"

#include <queue>

namespace rd {
	class Mq {
	public:
		//region ctor/dtor

		Mq() = default;

		Mq(Mq const &) = delete;

		Mq& operator=(Mq const &) = delete;

		Mq(Mq &&) = default;

		Mq& operator =(Mq &&) = default;
		//endregion

		mutable std::queue<Buffer> default_scheduler_messages;
		std::vector<Buffer> custom_scheduler_messages;
	};

	class MessageBroker final {
	private:
		IScheduler *default_scheduler = nullptr;
		mutable rd::unordered_map<RdId, IRdReactive const *> subscriptions;
		mutable rd::unordered_map<RdId, Mq> broker;

		mutable std::recursive_mutex lock;

		static Logger logger;

		void invoke(const IRdReactive *that, Buffer msg, bool sync = false) const;

	public:

		//region ctor/dtor

		explicit MessageBroker(IScheduler *defaultScheduler);
		//endregion

		void dispatch(RdId id, Buffer message) const;

		void advise_on(Lifetime lifetime, IRdReactive const *entity) const;
	};
}

#endif //RD_CPP_MESSAGEBROKER_H
