#include "MessageBroker.h"

#include "core_util.h"
#include "Buffer.h"

#include <cassert>



namespace rd {
	Logger MessageBroker::logger;

	void MessageBroker::invoke(const IRdReactive *that, Buffer msg, bool sync) const {
		if (sync) {
			that->on_wire_received(std::move(msg));
		} else {
			auto action = [this, that, message = std::move(msg)]() mutable {
				bool exists_id = false;
				{
					std::lock_guard<decltype(lock)> guard(lock);
					exists_id = subscriptions.count(that->rdid) > 0;
				}
				if (exists_id) {
					that->on_wire_received(std::move(message));
				} else {
					logger.trace("Disappeared Handler for Reactive entity with id:" + that->rdid.toString());
				}
			};
			std::function<void()> function = util::make_shared_function(std::move(action));
			that->get_wire_scheduler()->queue(std::move(function));
		}
	}

	MessageBroker::MessageBroker(IScheduler *defaultScheduler) : defaultScheduler(defaultScheduler) {}

	void MessageBroker::dispatch(RdId id, Buffer message) const {
		MY_ASSERT_MSG(!id.isNull(), "id mustn't be null");

		{//synchronized recursively
			std::lock_guard<decltype(lock)> guard(lock);
			IRdReactive const *s = subscriptions[id];
			if (s == nullptr) {
				if (broker.count(id) == 0) {
					broker[id];
				}
				broker[id].defaultSchedulerMessages++;

				auto action = [this, id, message = std::move(message)]() mutable {
					IRdReactive const *subscription = subscriptions[id];

					if (subscription != nullptr) {
						if (subscription->get_wire_scheduler() == defaultScheduler)
							invoke(subscription, std::move(message), true);
						else
							invoke(subscription, std::move(message));
					} else {
						logger.trace("No handler for id: " + id.toString());
					}

					if (--broker[id].defaultSchedulerMessages == 0) {
						auto t = std::move(broker[id]);
						broker.erase(id);
						for (auto &it : t.customSchedulerMessages) {
							assert(subscription->get_wire_scheduler() != defaultScheduler);
							invoke(subscription, std::move(it));
						}
					}
				};
				std::function<void()> function = util::make_shared_function(std::move(action));
				defaultScheduler->queue(std::move(function));
			} else {
				if (s->get_wire_scheduler() == defaultScheduler || s->get_wire_scheduler()->out_of_order_execution) {
					invoke(s, std::move(message));
				} else {
					if (broker.count(id) == 0) {
						invoke(s, std::move(message));
					} else {
						Mq &mq = broker[id];
						assert(mq.defaultSchedulerMessages > 0);
						mq.customSchedulerMessages.push_back(std::move(message));
					}
				}
			}
		}

		//        }
	}

	void MessageBroker::advise_on(Lifetime lifetime, IRdReactive const *entity) const {
		MY_ASSERT_MSG(!entity->rdid.isNull(), ("id is null for entity: " + std::string(typeid(*entity).name())));

		//advise MUST happen under default scheduler, not custom
		defaultScheduler->assert_thread();

		std::lock_guard<decltype(lock)> guard(lock);
		if (!lifetime->is_terminated()) {
			auto key = entity->rdid;
			IRdReactive const *value = entity;
			subscriptions[key] = value;
			lifetime->add_action([this, key]() {
				subscriptions.erase(key);
			});
		}
	}
}

