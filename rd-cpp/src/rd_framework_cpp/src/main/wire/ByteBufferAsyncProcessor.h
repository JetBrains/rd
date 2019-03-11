//
// Created by jetbrains on 15.09.2018.
//

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

		std::future<void> asyncFuture;

		Buffer::ByteArray data;
	public:

		//region ctor/dtor

		ByteBufferAsyncProcessor(std::string id, std::function<void(Buffer::ByteArray)> processor);

		//endregion

	private:
		void cleanup0();

		bool terminate0(time_t timeout, StateKind stateToSet, const std::string &action);

		void ThreadProc();

	public:
		void start();

		bool stop(time_t timeout = time_t(0));

		bool terminate(time_t timeout = time_t(0)/*InfiniteDuration*/);

		void put(Buffer::ByteArray newData);
	};

	std::string to_string(ByteBufferAsyncProcessor::StateKind state);
}

#endif //RD_CPP_BYTEBUFFERASYNCPROCESSOR_H
