#ifndef RD_CPP_RDENDPOINT_H
#define RD_CPP_RDENDPOINT_H

#include "serialization/Polymorphic.h"
#include "RdTask.h"
#include "framework_traits.h"

namespace rd
{
namespace detail
{
/// \brief For bindable result
template <typename>
class RdEndpointWiredResult;

template <typename T>
class RdEndpointWiredResult<Wrapper<T>> final : public RdReactiveBase
{
	friend class EndpointWiredRdTask;

	LifetimeDefinition result_lifetime_def;
	Wrapper<T> value;
public:
	explicit RdEndpointWiredResult(LifetimeDefinition result_lifetime_def, const Wrapper<T>& value) : result_lifetime_def(std::move(result_lifetime_def)), value(value)
	{
	}

	RdEndpointWiredResult(const RdEndpointWiredResult&) = delete;				// non construction-copyable
	RdEndpointWiredResult& operator=(const RdEndpointWiredResult&) = delete;	// non copyable

	void init(const Lifetime lifetime) const override
	{
		RdReactiveBase::init(lifetime);
		get_wire()->advise(lifetime, this);
	}

	void bind(const Lifetime lf, IRdDynamic const* parent, string_view name) const override
	{
		RdReactiveBase::bind(lf, parent, name);
		value->bind(lf, this, "Value");
	}

	void identify(Identities const& identities, RdId const& id) const override
	{
		RdReactiveBase::identify(identities, id);
		value->identify(identities, id.mix("Value"));
	}

	void on_wire_received(Buffer buffer) const override
	{
		spdlog::get("logReceived")->trace("received cancellation");

		//nothing just a void value

		get_wire_scheduler()->invoke_or_queue([this] { result_lifetime_def.terminate(); });
	}
};

template <typename T, typename S>
class RdEndpointTaskCancellation : public RdReactiveBase
{
	using Task = RdTask<T, S>;

	Task task;

public:
	explicit RdEndpointTaskCancellation(Task task) : task(task)
	{
	}

	RdEndpointTaskCancellation(const RdEndpointTaskCancellation&) = delete;				// non construction-copyable
	RdEndpointTaskCancellation& operator=(const RdEndpointTaskCancellation&) = delete;	// non copyable

	void init(const Lifetime lifetime) const override
	{
		RdReactiveBase::init(lifetime);
		get_wire()->advise(lifetime, this);
	}

	void on_wire_received(Buffer buffer) const override
	{
		spdlog::get("logReceived")->trace("received cancellation");

		//nothing just a void value

		get_wire_scheduler()->invoke_or_queue([this] { task.set_result_if_empty(typename Task::result_type::Cancelled()); });
	}
};

}

/**
 * \brief An API that is exposed to the remote process and can be invoked over the protocol.
 *
 * \tparam TReq type of request
 * \tparam TRes type of response
 * \tparam ReqSer "SerDes" for request
 * \tparam ResSer "SerDes" for response
 */
template <typename TReq, typename TRes, typename ReqSer = Polymorphic<TReq>, typename ResSer = Polymorphic<TRes>>
class RdEndpoint : public virtual RdReactiveBase, public ISerializable
{
	using WTReq = value_or_wrapper<TReq>;
	using WTRes = value_or_wrapper<TRes>;
	using Task = RdTask<TRes, ResSer>;
	using TaskResult = typename Task::result_type;

	using handler_t = std::function<RdTask<TRes, ResSer>(Lifetime result_lifetime, TReq const&)>;
	mutable handler_t local_handler;

	void send_result(const RdId task_id, const TaskResult& result) const
	{
		auto logger = spdlog::get("logSend");
		if (logger->should_log(spdlog::level::trace))
			logger->trace("endpoint {}::{} response = {}", to_string(get_location()), to_string(get_id()), to_string(result));
		get_wire()->send(task_id, [this, &result](Buffer& inner_buffer) { result.write(get_serialization_context(), inner_buffer); });
	}

	template <class Bindable = TRes, std::enable_if_t<util::is_bindable_v<Bindable>, bool> = true>
	void handle_result(LifetimeDefinition result_lifetime_def, const RdId task_id, const TaskResult& result) const
	{
		if (result.is_succeeded())
		{
			try
			{
				auto result_lifetime = result_lifetime_def.lifetime;
				auto wired_result = result_lifetime->make_attached<detail::RdEndpointWiredResult<WTRes>>(std::move(result_lifetime_def), result.get_value());
				wired_result->identify(*get_protocol()->get_identity(), task_id);
				wired_result->bind(result_lifetime, this, "EndpointWiredResult");
				send_result(task_id, result);
			}
			catch (const std::exception& ex)
			{
				spdlog::get("logSend")->error(ex.what());
				send_result(task_id, typename TaskResult::Fault(ex));
			}
		}
		else
		{
			send_result(task_id, result);
		}
	}

	template <class NonBindable = TRes, std::enable_if_t<!util::is_bindable_v<NonBindable>, bool> = true>
	void handle_result(LifetimeDefinition /*should_be_destroyed_on_complete*/, const RdId task_id, TaskResult result) const
	{
		try
		{
			send_result(task_id, result);
		}
		catch (const std::exception& ex)
		{
			spdlog::get("logSend")->error(ex.what());
			if (result.is_succeeded())
				send_result(task_id, typename TaskResult::Fault(ex));
		}
	}

	void handle_result_async(LifetimeDefinition result_lifetime_def, const RdId task_id, Task task) const
	{
		struct AsyncCallData
		{
			const RdEndpoint* endpoint;
			LifetimeDefinition call_lifetime_def;
			LifetimeDefinition result_lifetime_def;
			RdId task_id;

			AsyncCallData(const RdEndpoint* endpoint, LifetimeDefinition call_lifetime_def, LifetimeDefinition result_lifetime_def, const RdId task_id) :
				endpoint(endpoint), call_lifetime_def(std::move(call_lifetime_def)), result_lifetime_def(std::move(result_lifetime_def)), task_id(task_id)
			{
			}
		};

		auto call_lifetime_def = LifetimeDefinition(*bind_lifetime);
		auto call_lifetime = call_lifetime_def.lifetime;
		auto cancellation = call_lifetime->make_attached<detail::RdEndpointTaskCancellation<TRes, ResSer>>(task);
		cancellation->set_id(task_id);
		cancellation->bind(call_lifetime, this, "EndpointTaskCancellation");

		task.advise(call_lifetime, [data = std::make_shared<AsyncCallData>(this, std::move(call_lifetime_def), std::move(result_lifetime_def), task_id)](const auto& result)
		{
			data->call_lifetime_def.terminate();
			data->endpoint->handle_result(std::move(data->result_lifetime_def), data->task_id, result);
		});
	}
public:
	// region ctor/dtor

	RdEndpoint() = default;

	explicit RdEndpoint(handler_t handler)
	{
		set(std::move(handler));
	}

	explicit RdEndpoint(std::function<WTRes(TReq const&)> handler)
	{
		set(std::move(handler));
	}

	RdEndpoint(RdEndpoint&&) = default;

	RdEndpoint& operator=(RdEndpoint&&) = default;

	virtual ~RdEndpoint() = default;
	// endregion

	static RdEndpoint<TReq, TRes, ReqSer, ResSer> read(SerializationCtx& /*ctx*/, Buffer& buffer)
	{
		RdEndpoint<TReq, TRes, ReqSer, ResSer> res;
		const RdId& id = RdId::read(buffer);
		withId(res, id);
		return res;
	}

	void write(SerializationCtx& /*ctx*/, Buffer& buffer) const override
	{
		rdid.write(buffer);
	}

	/**
	 * \brief Assigns a handler that executes the API asynchronously.
	 * \param handler to assign
	 */
	void set(handler_t handler) const
	{
		RD_ASSERT_MSG(handler, "handler is set already");
		local_handler = std::move(handler);
	}

	/**
	 * \brief @see set above
	 */
	void set(std::function<WTRes(TReq const&)> functor) const
	{
		local_handler = [handler = std::move(functor)](Lifetime _, TReq const& req)
		{
			return Task::from_result(handler(req));
		};
	}

	/**
	 * \brief @see set above
	 */
	void set(std::function<WTRes(const Lifetime& lifetime, TReq const&)> functor) const
	{
		local_handler = [handler = std::move(functor)](const Lifetime& lifetime, TReq const& req)
		{
			return Task::from_result(handler(lifetime, req));
		};
	}

	void init(Lifetime lifetime) const override
	{
		RdReactiveBase::init(lifetime);
		bind_lifetime = lifetime;
		get_wire()->advise(lifetime, this);
	}

	void on_wire_received(Buffer buffer) const override
	{
		auto task_id = RdId::read(buffer);
		auto value = ReqSer::read(get_serialization_context(), buffer);
		spdlog::get("logReceived")->trace("endpoint {}::{} request = {}", to_string(location), to_string(rdid), to_string(value));
		if (!local_handler)
		{
			throw std::invalid_argument("handler is empty for RdEndPoint");
		}

		auto result_lifetime_def = LifetimeDefinition(*bind_lifetime);
		try
		{
			auto task = local_handler(result_lifetime_def.lifetime, wrapper::get<TReq>(value));
			if (!task.has_value())
				handle_result_async(std::move(result_lifetime_def), task_id, task);
			else
				handle_result(std::move(result_lifetime_def), task_id, task.value_or_throw());
		}
		catch (std::exception const& e)
		{
			send_result(task_id, typename TaskResult::Fault(e));
		}
	}

	friend bool operator==(const RdEndpoint& lhs, const RdEndpoint& rhs)
	{
		return &lhs == &rhs;
	}

	friend bool operator!=(const RdEndpoint& lhs, const RdEndpoint& rhs)
	{
		return !(rhs == lhs);
	}

	friend std::string to_string(RdEndpoint const& /*value*/)
	{
		return "RdEndpoint";
	}
};
}	 // namespace rd

#endif	  // RD_CPP_RDENDPOINT_H
