#ifndef RD_CPP_RDENDPOINT_H
#define RD_CPP_RDENDPOINT_H

#include "serialization/Polymorphic.h"
#include "RdTask.h"

namespace rd
{
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

	using handler_t = std::function<RdTask<TRes, ResSer>(Lifetime, TReq const&)>;
	mutable handler_t local_handler;

	mutable tsl::ordered_map<RdId, RdTask<TRes, ResSer>, rd::hash<RdId>> awaiting_tasks;	// TO-DO get rid of it
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
		local_handler = [handler = std::move(functor)](Lifetime _, TReq const& req) -> RdTask<TRes, ResSer>
		{ return RdTask<TRes, ResSer>::from_result(handler(req)); };
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

		using TaskResult = RdTaskResult<TRes, ResSer>;

		auto lifetime = *bind_lifetime;
		auto send_result = [this, task_id](TaskResult const& task_result)
		{
			auto logger = spdlog::get("logSend");
			if (logger->should_log(spdlog::level::trace))
				logger->trace("endpoint {}::{} response = {}", to_string(location), to_string(rdid), to_string(task_result));
			get_wire()->send(task_id, [&](Buffer& inner_buffer) { task_result.write(get_serialization_context(), inner_buffer); });
		};

		try
		{
			auto task = local_handler(lifetime, wrapper::get<TReq>(value));
			awaiting_tasks[task_id] = task;
			lifetime->add_action([this, task_id] { awaiting_tasks.erase(task_id); });
			task.advise(lifetime, send_result);
		}
		catch (std::exception const& e)
		{
			send_result(typename TaskResult::Fault(e));
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
