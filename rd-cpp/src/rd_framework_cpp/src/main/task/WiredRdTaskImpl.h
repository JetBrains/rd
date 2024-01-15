#ifndef RD_CPP_WIREDRDTASKIMPL_H
#define RD_CPP_WIREDRDTASKIMPL_H

#include "serialization/Polymorphic.h"
#include "RdTaskResult.h"
#include "util/framework_traits.h"
#include "util/lifetime_util.h"

namespace rd
{
template <typename, typename>
class WiredRdTask;

namespace detail
{
template <typename T, typename S = Polymorphic<T>>
class WiredRdTaskImpl : public RdReactiveBase
{
	using Task = RdTask<T, S>;
	using TaskResult = typename Task::result_type;

	Lifetime lifetime;
	RdReactiveBase const* cutpoint{};
	IScheduler* scheduler{};
	Property<TaskResult>* result{};

	LifetimeImpl::counter_t termination_lifetime_id{};

	template <class Bindable = T, std::enable_if_t<util::is_bindable_v<Bindable>, bool> = true>
	TaskResult bind_result(TaskResult task_result) const
	{
		if (!task_result.is_succeeded())
			return task_result;

		auto lifetime_defintion = LifetimeDefinition(lifetime);
		auto result_lifetime = lifetime_defintion.lifetime;
		auto value = util::attach_lifetime(task_result.get_value(), std::move(lifetime_defintion));
		result_lifetime->add_action([task_id = get_id(), cutpoint = cutpoint]
		{
			cutpoint->get_wire()->send(task_id, [](auto&)
			{
				 // write nothing, just signal server to release result lifetime
			});
		});
		value->bind(result_lifetime, cutpoint, "CallResult");
		return typename TaskResult::Success(value);
	}

	template <class NonBindable = T, std::enable_if_t<!util::is_bindable_v<NonBindable>, bool> = true>
	TaskResult bind_result(TaskResult result) const
	{
		return result;
	}

public:
	template <typename, typename>
	friend class ::rd::WiredRdTask;

	WiredRdTaskImpl(
		Lifetime lifetime, RdReactiveBase const& cutpoint, RdId rdid, IScheduler* scheduler, Property<TaskResult>* result)
		: lifetime(lifetime), cutpoint(&cutpoint), scheduler(scheduler), result(result)
	{
		this->rdid = std::move(rdid);
		cutpoint.get_wire()->advise(lifetime, this);
		termination_lifetime_id =
			lifetime->add_action([this]() { this->result->set_if_empty(typename TaskResult::Cancelled{}); });
	}

	virtual ~WiredRdTaskImpl()
	{
		lifetime->remove_action(termination_lifetime_id);
	}

	void on_wire_received(Buffer buffer) const override
	{
		auto read_result = RdTaskResult<T, S>::read(cutpoint->get_serialization_context(), buffer);
		spdlog::get("logReceived")
			->trace("call {} {} received response {} : {}", to_string(cutpoint->get_location()), to_string(rdid), to_string(rdid),
				to_string(read_result));
		scheduler->queue([&, result = std::move(read_result)]() mutable {
			if (this->result->has_value())
			{
				spdlog::get("logReceived")->trace("call {} {} response was dropped, task result is: {}", to_string(location), to_string(rdid),
					to_string(result.unwrap()));
			}
			else
			{
				result = bind_result(std::move(result));
				this->result->set_if_empty(std::move(result));
			}
		});
	}

	IScheduler* get_wire_scheduler() const override
	{
		return &SynchronousScheduler::Instance();
	}
};
}	 // namespace detail
}	 // namespace rd

#endif	  // RD_CPP_WIREDRDTASKIMPL_H
