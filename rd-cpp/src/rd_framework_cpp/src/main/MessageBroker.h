#ifndef RD_CPP_MESSAGEBROKER_H
#define RD_CPP_MESSAGEBROKER_H

#include "IRdReactive.h"
#include "Logger.h"

#include <unordered_map>

namespace rd {
	class Mq {
	public:
		//region ctor/dtor

		Mq() = default;

		Mq(Mq const &) = delete;

		Mq(Mq &&) = default;
		//endregion

		int32_t defaultSchedulerMessages = 0;
		std::vector<Buffer> customSchedulerMessages;
	};

	class MessageBroker final {
	private:
		IScheduler *defaultScheduler = nullptr;
		mutable std::unordered_map<RdId, IRdReactive const *> subscriptions;
		mutable std::unordered_map<RdId, Mq> broker;

		mutable std::recursive_mutex lock;

		static Logger logger;

		void invoke(const IRdReactive *that, Buffer msg, bool sync = false) const;

	public:

		//region Description

		MessageBroker(MessageBroker &&) = default;

		explicit MessageBroker(IScheduler *defaultScheduler);
		//endregion

		void dispatch(RdId id, Buffer message) const;

		void advise_on(Lifetime lifetime, IRdReactive const *entity) const;
	};
}

#endif //RD_CPP_MESSAGEBROKER_H
