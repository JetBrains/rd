#ifndef RD_CPP_RDCALL_H
#define RD_CPP_RDCALL_H

#include "Polymorphic.h"
#include "RdTask.h"
#include "RdTaskResult.h"
#include "SynchronousScheduler.h"

#include <thread>

#pragma warning( push )
#pragma warning( disable:4250 )

namespace rd {
	/**
	 * \brief Represents an API provided by the remote process which can be invoked through the protocol. 
	 *
	 * \tparam TReq type of request
	 * \tparam TRes type of response
	 * \tparam ReqSer "SerDes" for request
	 * \tparam ResSer "SerDes" for response
	 */
	template<typename TReq, typename TRes, typename ReqSer = Polymorphic<TReq>, typename ResSer = Polymorphic<TRes> >
	class RdCall final : public RdReactiveBase, public ISerializable {
		using WTReq = value_or_wrapper<TReq>;
		using WTRes = value_or_wrapper<TRes>;

		mutable std::unordered_map<RdId, std::pair<IScheduler *, RdTask<TRes, ResSer>>> requests;
		mutable optional<RdId> syncTaskId;

		mutable RdTask<TRes, ResSer> last_task;
	public:

		//region ctor/dtor
		RdCall() = default;

		RdCall(RdCall &&) = default;

		RdCall &operator=(RdCall &&) = default;

		virtual ~RdCall() = default;
		//endregion

		static RdCall<TReq, TRes, ReqSer, ResSer> read(SerializationCtx const &ctx, Buffer const &buffer) {
			RdCall<TReq, TRes, ReqSer, ResSer> res;
			const RdId &id = RdId::read(buffer);
			withId(res, id);
			return res;
		}

		void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
			rdid.write(buffer);
		}

		void init(Lifetime lifetime) const override {
			RdBindableBase::init(lifetime);

			get_wire()->advise(lifetime, this);

			lifetime->add_action([this]() {
				for (auto const &req : requests) {
					RdTask<TRes, ResSer> task = req.second.second;
					if (!task.has_value()) {//todo race
						task.cancel();
					}
				}

				requests.clear();
			});
		}

		void on_wire_received(Buffer buffer) const override {
			RdId taskId = RdId::read(buffer);

			if (requests.count(taskId) == 0) {
				logReceived.trace("call " + location.toString() + " " + rdid.toString() +
								  "received response " + taskId.toString() + " but it was dropped");
			} else {
				auto const &request = requests.at(taskId);
				auto result = RdTaskResult<TRes, ResSer>::read(get_serialization_context(), buffer);
				logReceived.trace("call " + location.toString() + " " + rdid.toString() +
								  " received response " + taskId.toString() + " : " + to_string(result));
				//auto const&[scheduler, task] = request;
				auto const &scheduler = request.first;
				auto const &task = request.second;
				scheduler->queue([&, result = std::move(result)]() mutable {
					if (task.has_value()) {
						logReceived.trace("call " + location.toString() + " " + rdid.toString() +
										  " response was dropped, task result is: " + to_string(result.unwrap()));
						if (is_bound() && get_default_scheduler()->is_active() && requests.count(taskId) > 0) {
							//                        logAssert.error("MainThread: ${defaultScheduler.isActive}, taskId=$taskId ");
						}
					} else {
						//todo could be race condition in sync mode in case of Timeout, but it's not really valid case
						//todo but now we could start task on any scheduler - need interlocks in property
						task.set_result(std::move(result));
						requests.erase(taskId);
					}
				});
			}
		}

		/**
		 * \brief Invokes the API with the parameters given as [request] and waits for the result.
		 * 
		 * \param request value to deliver 
		 * \return result of remote invoking
		 */
		TRes const &sync(TReq const &request) const {
			try {
				last_task = start_internal(request, true, get_default_scheduler());
				while (!last_task.has_value()) {
					std::this_thread::yield();
				}
				auto const &res = last_task.value_or_throw().unwrap();
				syncTaskId = nullopt;
				return res;
			} catch (...) {
				throw;
			}
		}


		IScheduler *get_wire_scheduler() const override {
			return &globalSynchronousScheduler;
		}

		/**
		 * \brief Asynchronously invokes the API with the parameters given as [request] and waits for the result.
		 *                      
		 * \param request value of request
		 * \param responseScheduler to assign value
		 * \return task which will have its result value.
		 */
		RdTask<TRes, ResSer> start(TReq const &request, IScheduler *responseScheduler = nullptr) const {
			return start_internal(request, false, responseScheduler ? responseScheduler : get_default_scheduler());
		}
	private:
		RdTask<TRes, ResSer> start_internal(TReq const &request, bool sync, IScheduler *scheduler) const {
			assert_bound();
			if (!async) {
				assert_threading();
			}

			RdId taskId = get_protocol()->get_identity()->next(rdid);
			RD_ASSERT_MSG(requests.count(taskId) == 0, "requests already contain task with id:" + taskId.toString());
			RdTask<TRes, ResSer> task;
			auto pair = std::make_pair(scheduler, task);
			requests.emplace(taskId, std::move(pair));

			if (sync) {
				if (syncTaskId.has_value()) {
					throw std::invalid_argument(
							"Already exists sync task for call " + location.toString() + ", taskId = " +
							(*syncTaskId).toString());
				}
				syncTaskId = taskId;
			}

			get_wire()->send(rdid, [&](Buffer const &buffer) {
				logSend.trace(
						"call " + location.toString() + "::" + rdid.toString() + " send" + (sync ? "SYNC" : "ASYNC") +
						" request " + taskId.toString() + " : " + to_string(request));
				taskId.write(buffer);
				ReqSer::write(get_serialization_context(), buffer, request);
			});

			return task;
		}
	};
}

#pragma warning( pop )

#endif //RD_CPP_RDCALL_H
