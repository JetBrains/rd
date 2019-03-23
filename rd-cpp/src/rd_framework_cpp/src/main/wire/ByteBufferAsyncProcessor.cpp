//
// Created by jetbrains on 15.09.2018.
//

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

	bool ByteBufferAsyncProcessor::terminate0(time_t timeout, StateKind stateToSet, const std::string &action) {
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

		std::future_status status = asyncFuture.wait_for(timeout);

		if (status == std::future_status::timeout) {
			logger.error("Couldn't wait async thread during time:" + to_string(timeout));
		}

		cleanup0();

		return true;
	}

	void ByteBufferAsyncProcessor::ThreadProc() {
		while (true) {
			Buffer::ByteArray send_buffer;
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
				send_buffer = std::move(data);
			}

			try {
				processor(std::move(send_buffer));
				clear_data();
			} catch (std::exception const &e) {
				logger.error("Exception while processing byte queue", &e);
				clear_data();
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

			asyncFuture = std::async(std::launch::async, &ByteBufferAsyncProcessor::ThreadProc, this);
		}
	}

	bool ByteBufferAsyncProcessor::stop(time_t timeout) {
		return terminate0(timeout, StateKind::Stopping, "STOP");
	}

	bool ByteBufferAsyncProcessor::terminate(time_t timeout) {
		return terminate0(timeout, StateKind::Terminating, "TERMINATE");
	}

	void ByteBufferAsyncProcessor::put(Buffer::ByteArray newData) {
		{
			std::lock_guard<decltype(lock)> guard(lock);

			if (state >= StateKind::Stopping) {
				return;
			}
			data.insert(data.end(), newData.begin(), newData.end());
			//todo change to cyclic array?

			cv.notify_all();
		}
	}

	void ByteBufferAsyncProcessor::clear_data() {
		std::lock_guard<decltype(lock)> guard(lock);
		data.clear();
		data.shrink_to_fit();
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
