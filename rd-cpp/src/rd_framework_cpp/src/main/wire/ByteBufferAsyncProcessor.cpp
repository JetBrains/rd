#include "ByteBufferAsyncProcessor.h"

namespace rd {
	size_t ByteBufferAsyncProcessor::INITIAL_CAPACITY = 1024 * 1024;

	Logger ByteBufferAsyncProcessor::logger;

	ByteBufferAsyncProcessor::ByteBufferAsyncProcessor(std::string id, std::function<void(Buffer::ByteArray)> processor)
			: id(std::move(id)), processor(std::move(processor)) {
		data.reserve(INITIAL_CAPACITY);
	}

	void ByteBufferAsyncProcessor::cleanup0() {
		state = StateKind::Terminated;
		cv.notify_all();
	}

	bool ByteBufferAsyncProcessor::terminate0(time_t timeout, StateKind stateToSet, string_view action) {
		{
			std::lock_guard<decltype(lock)> guard(lock);
			if (state == StateKind::Initialized) {
				logger.debug("Can't action \'" + id + "\', because it hasn't been started yet");
				cleanup0();
				return true;
			}

			if (state >= stateToSet) {
				logger.debug("Trying to action async processor \'" + id + "\' but it's in state " + to_string(state));
				return true;
			}

			state = stateToSet;
			cv.notify_all();
		}

		std::future_status status = async_future.wait_for(timeout);

		if (status == std::future_status::timeout) {
			logger.error("Couldn't wait async thread during time:" + to_string(timeout));
		}

		cleanup0();

		return true;
	}

	void ByteBufferAsyncProcessor::ThreadProc() {
		async_thread_id = std::this_thread::get_id();

		while (true) {
			decltype(data) data_to_send;
			{
				std::lock_guard<decltype(lock)> guard(lock);

				if (state >= StateKind::Terminated) {
					return;
				}

				while (data.empty()) {
					if (state >= StateKind::Stopping) {
						return;
					}
					cv.wait(lock);
					if (state >= StateKind::Terminating) {
						return;
					}
				}

				data_to_send = std::move(data);
			}

			try {
				for (auto &&item : data_to_send) {
					processor(std::move(item));
				}
			} catch (std::exception const &e) {
				logger.error("Exception while processing byte queue", &e);
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

			cv.notify_all();
		}
	}

	void ByteBufferAsyncProcessor::pause(const std::string &reason) {
		std::lock_guard<decltype(lock)> guard(lock);

		logger.debug(id + " paused with reason" + reason + ",state=" + to_string(state));

		if (std::this_thread::get_id() != async_thread_id) {
			//todo
		}
		//todo
	}

	void ByteBufferAsyncProcessor::resume(const std::string &reason) {
		std::lock_guard<decltype(lock)> guard(lock);

		logger.debug(id + " resumed with reason:" + reason);

		cv.notify_all();
	}

	void ByteBufferAsyncProcessor::acknowledge(sequence_number_t seqn) {
		std::lock_guard<decltype(lock)> guard(lock);

		if (seqn > acknowledged_seqn) {
			logger.trace("New acknowledged seqn: " + std::to_string(seqn));
			acknowledged_seqn = seqn;
		} else {
			throw std::invalid_argument(
					"Acknowledge " + std::to_string(seqn) + " called," +
					"while next seqn MUST BE greater than " + std::to_string(acknowledged_seqn)
			);
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
