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
				logReceived.trace("call %s with id=%s received response %s but it was dropped",
								  location.toString().c_str(),
								  rdid.toString().c_str(),
								  taskId.toString().c_str());
			} else {
				auto const &request = requests.at(taskId);
				auto result = RdTaskResult<TRes, ResSer>::read(get_serialization_context(), buffer);
				logReceived.trace("call %s with id=%s received response %s ",
								  location.toString().c_str(),
								  rdid.toString().c_str(),
								  taskId.toString().c_str());
				//auto const&[scheduler, task] = request;
				auto const &scheduler = request.first;
				auto const &task = request.second;
				scheduler->queue([&, result = std::move(result)]() mutable {
					if (task.has_value()) {
						logReceived.trace("call %s with id=%s, response was dropped, task result is %s",
										  location.toString().c_str(),
										  rdid.toString().c_str(),
										  to_string(result.unwrap()).c_str());
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

		TRes const &sync(TReq const &request) const {
			try {
				last_task = startInternal(request, true, get_default_scheduler());
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

		RdTask<TRes, ResSer> start(TReq const &request, IScheduler *responseScheduler) const {
			return startInternal(request, false, responseScheduler ? responseScheduler : get_default_scheduler());
		}

		RdTask<TRes, ResSer> start(TReq const &request) const {
			return start(request, nullptr);
		}

	private:
		RdTask<TRes, ResSer> startInternal(TReq const &request, bool sync, IScheduler *scheduler) const {
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
