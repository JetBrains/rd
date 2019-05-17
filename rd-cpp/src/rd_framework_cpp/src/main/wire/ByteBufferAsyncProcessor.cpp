#include <util/guards.h>
#include "ByteBufferAsyncProcessor.h"

namespace rd {
	size_t ByteBufferAsyncProcessor::INITIAL_CAPACITY = 1024 * 1024;

	Logger ByteBufferAsyncProcessor::logger;

	ByteBufferAsyncProcessor::ByteBufferAsyncProcessor(
			std::string id, std::function<bool(Buffer::ByteArray const &, sequence_number_t)> processor)
			: id(std::move(id)), processor(std::move(processor)) {
		data.reserve(INITIAL_CAPACITY);
	}

	void ByteBufferAsyncProcessor::cleanup0() {
		{
			std::lock_guard<decltype(lock)> guard(lock);

			state = StateKind::Terminated;
		}
		//todo clean data

		cv.notify_all();
	}

	bool ByteBufferAsyncProcessor::terminate0(time_t timeout, StateKind state_to_set, string_view action) {
		{
			std::lock_guard<decltype(lock)> guard(lock);
			if (state == StateKind::Initialized) {
				logger.debug("Can't " + std::string(action) + "\'" + id + "\', because it hasn't been started yet");
				cleanup0();
				return true;
			}

			if (state >= state_to_set) {
				logger.debug("Trying to " + std::string(action) + " async processor \'" + id + "\' but it's in state " +
							 to_string(state));
				return true;
			}

			state = state_to_set;
		}
		cv.notify_all();

		std::future_status status = async_future.wait_for(timeout);

		bool success = true;

		if (status == std::future_status::timeout) {
			logger.error("Couldn't wait async thread during time: %s", to_string(timeout).c_str());
			success = false;
		}

		cleanup0();

		return success;
	}

	void ByteBufferAsyncProcessor::add_data(std::vector<Buffer::ByteArray> &&new_data) {
		std::lock_guard<decltype(queue_lock)> guard(queue_lock);
		for (auto &&item : new_data) {
			queue.emplace(std::move(item));
			//todo bulk
		}
	}

	bool ByteBufferAsyncProcessor::reprocess() {
		{
			std::lock_guard<decltype(queue_lock)> guard(queue_lock);

			logger.debug(this->id + ": reprocessing started");

			cv.wait(lock, [this]() -> bool {
				return !in_processing;
			});

			logger.debug(this->id + ": reprocessing waited for main processing");

			while (current_seqn <= acknowledged_seqn) {
				pending_queue.pop_front();
				++current_seqn;
			}
			for (int i = 0; i < pending_queue.size(); ++i) {
				auto const &item = pending_queue[i];
				if (!processor(item, current_seqn + i)) {
					return false;
				}
			}
		}
		return true;
	}

	void ByteBufferAsyncProcessor::process() {
		{
			std::lock_guard<decltype(queue_lock)> guard(queue_lock);
			util::bool_guard bool_guard(in_processing);

			logger.debug(this->id + ": processing started");

			while (!queue.empty() && processor(queue.front(), max_sent_seqn + 1)) {
				++max_sent_seqn;
				pending_queue.push_back(std::move(queue.front()));
				queue.pop();
			}
		}

		cv.notify_all();
	}

	void ByteBufferAsyncProcessor::ThreadProc() {
		async_thread_id = std::this_thread::get_id();

		while (true) {
			{
				std::lock_guard<decltype(lock)> guard(lock);

				if (state >= StateKind::Terminated) {
					return;
				}

				while (data.empty() || interrupt_balance != 0) {
					if (state >= StateKind::Stopping) {
						return;
					}
					cv.wait(lock);

					logger.debug(this->id + "'s ThreadProc waited for notify");

					if (state >= StateKind::Terminating) {
						return;
					}
				}
				add_data(std::move(data));
				data.clear();
			}

			try {
				process();
			} catch (std::exception const &e) {
				logger.error(&e, "Exception while processing byte queue");
			}
		}
	}

	void ByteBufferAsyncProcessor::start() {
		{
			std::lock_guard<decltype(lock)> guard(lock);

			if (state != StateKind::Initialized) {
				logger.debug("Trying to START async processor " + id + " but it's in state " + to_string(state));
				return;
			}

			state = StateKind::AsyncProcessing;

			async_future = std::async(std::launch::async, &ByteBufferAsyncProcessor::ThreadProc, this);
		}
	}

	bool ByteBufferAsyncProcessor::stop(time_t timeout) {
		return terminate0(timeout, StateKind::Stopping, "STOP");
	}

	bool ByteBufferAsyncProcessor::terminate(time_t timeout) {
		return terminate0(timeout, StateKind::Terminating, "TERMINATE");
	}

	void ByteBufferAsyncProcessor::put(Buffer::ByteArray new_data) {
		{
			std::lock_guard<decltype(lock)> guard(lock);

			if (state >= StateKind::Stopping) {
				return;
			}
			data.emplace_back(std::move(new_data));

		}
		cv.notify_all();
	}

	void ByteBufferAsyncProcessor::pause(const std::string &reason) {
		std::lock_guard<decltype(lock)> guard(lock);

		++interrupt_balance;

		logger.debug(id + " paused with reason=" + reason + ",state=" + to_string(state));

		auto current_thread_id = std::this_thread::get_id();
		if (current_thread_id != async_thread_id) {
			logger.debug(id + " paused from another thread : " + to_string(current_thread_id));
			cv.wait(lock, [this]() -> bool {
				return !in_processing;
			});
			logger.debug(this->id + ": pausing waited for main processing");
		}
	}

	void ByteBufferAsyncProcessor::resume() {
		{
			std::lock_guard<decltype(lock)> guard(lock);

			reprocess();

			--interrupt_balance;

			logger.debug(id + " resumed");
		}

		cv.notify_all();
	}

	void ByteBufferAsyncProcessor::acknowledge(sequence_number_t seqn) {
		std::lock_guard<decltype(lock)> guard(lock);

		if (seqn > acknowledged_seqn) {
			logger.trace("New acknowledged seqn: %lld", seqn);
			acknowledged_seqn = seqn;
		} else {
			logger.error("Acknowledge %lld called, while next seqn MUST BE greater than %lld" , seqn, acknowledged_seqn);
		}
	}

	std::string to_string(ByteBufferAsyncProcessor::StateKind state) {
		switch (state) {
			case ByteBufferAsyncProcessor::StateKind::Initialized:
				return "Initialized";
			case ByteBufferAsyncProcessor::StateKind::AsyncProcessing:
				return "AsyncProcessing";
			case ByteBufferAsyncProcessor::StateKind::Stopping:
				return "Stopping";
			case ByteBufferAsyncProcessor::StateKind::Terminating:
				return "Terminating";
			case ByteBufferAsyncProcessor::StateKind::Terminated:
				return "Terminated";
		}
		return {};
	}
}
