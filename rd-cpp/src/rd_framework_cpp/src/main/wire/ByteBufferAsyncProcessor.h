#ifndef RD_CPP_BYTEBUFFERASYNCPROCESSOR_H
#define RD_CPP_BYTEBUFFERASYNCPROCESSOR_H

#include "Logger.h"
#include "Buffer.h"

#include <chrono>
#include <string>
#include <mutex>
#include <condition_variable>
#include <future>

namespace rd {
	using sequence_number_t = int64_t;

	class ByteBufferAsyncProcessor {
	public:
		enum class StateKind {
			Initialized,
			AsyncProcessing,
			Stopping,
			Terminating,
			Terminated
		};
	private:
		using time_t = std::chrono::milliseconds;

		static size_t INITIAL_CAPACITY;

		std::mutex lock;
		std::condition_variable_any cv;

		std::string id;

		std::function<void(Buffer::ByteArray)> processor;
		std::mutex processor_lock;
		std::condition_variable processor_cv;

		StateKind state{StateKind::Initialized};
		static Logger logger;

		std::thread::id async_thread_id;
		std::future<void> async_future;

		std::vector<Buffer::ByteArray> data;

		sequence_number_t acknowledged_seqn = 0;
	public:

		//region ctor/dtor

		ByteBufferAsyncProcessor(std::string id, std::function<void(Buffer::ByteArray)> processor);

		//endregion
	private:

		void cleanup0();

		bool terminate0(time_t timeout, StateKind stateToSet, string_view action);

		void ThreadProc();

	public:

		void start();

		bool stop(time_t timeout = time_t(0));

		bool terminate(time_t timeout = time_t(0)/*InfiniteDuration*/);

		void put(Buffer::ByteArray new_data);

		void pause(const std::string &reason);

		void resume(const std::string &reason);

		void acknowledge(int64_t seqn);
	};

	std::string to_string(ByteBufferAsyncProcessor::StateKind state);
}

#endif //RD_CPP_BYTEBUFFERASYNCPROCESSOR_H
