#ifndef RD_CPP_WIREDRDTASKIMPL_H
#define RD_CPP_WIREDRDTASKIMPL_H

#include "serialization/Polymorphic.h"
#include "RdTaskResult.h"

namespace rd {
	template<typename, typename>
	class WiredRdTask;

	namespace detail {
		template<typename T, typename S = Polymorphic <T>>
		class WiredRdTaskImpl : public RdReactiveBase {
		private:
			Lifetime lifetime;
			RdReactiveBase const *cutpoint{};
			IScheduler *scheduler{};
			Property <RdTaskResult<T, S>> *result{};

			LifetimeImpl::counter_t termination_lifetime_id{};
		public:
			template<typename, typename>
			friend
			class ::rd::WiredRdTask;


			WiredRdTaskImpl(Lifetime lifetime, RdReactiveBase const &cutpoint, RdId rdid, IScheduler *scheduler,
							Property <RdTaskResult<T, S>> *result)
					: lifetime(lifetime), cutpoint(&cutpoint), scheduler(scheduler), result(result) {
				this->rdid = std::move(rdid);
				cutpoint.get_wire()->advise(lifetime, this);
				termination_lifetime_id = lifetime->add_action([this]() {
					this->result->set_if_empty(typename RdTaskResult<T, S>::Cancelled{});
				});
			}

			virtual ~WiredRdTaskImpl() {
				lifetime->remove_action(termination_lifetime_id);
			}

			void on_wire_received(Buffer buffer) const override {
				auto read_result = RdTaskResult<T, S>::read(cutpoint->get_serialization_context(), buffer);
				logReceived.trace("call %s %s received response %s : " + to_string(read_result),
								  to_string(cutpoint->location).c_str(), to_string(rdid).c_str(), to_string(rdid).c_str());
				scheduler->queue([&, result = std::move(read_result)]() mutable {
					if (this->result->has_value()) {
						logReceived.trace(
								"call %s %s response was dropped, task result is: " + to_string(result.unwrap()),
								to_string(location).c_str(), to_string(rdid).c_str());
					} else {
						this->result->set_if_empty(std::move(result));
					}
				});
			}

			IScheduler *get_wire_scheduler() const override {
				return &globalSynchronousScheduler;
			}

		};
	}
}


#endif //RD_CPP_WIREDRDTASKIMPL_H
