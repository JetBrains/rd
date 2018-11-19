#include <utility>

//
// Created by jetbrains on 15.09.2018.
//

#ifndef RD_CPP_BYTEBUFFERASYNCPROCESSOR_H
#define RD_CPP_BYTEBUFFERASYNCPROCESSOR_H

#include "Logger.h"
#include "Buffer.h"

#include <chrono>
#include <string>
#include <thread>
#include <mutex>
#include <condition_variable>


class ByteArraySlice {
public:
    Buffer::ByteArray data;
    int32_t offset;
    int32_t len;

    ByteArraySlice(Buffer::ByteArray data, int32_t offset, int32_t len) : data(std::move(data)), offset(offset),
                                                                          len(len) {}
};

class ByteBufferAsyncProcessor {

    enum class StateKind {
        Initialized,
        AsyncProcessing,
        Stopping,
        Terminating,
        Terminated
    };

    using time_t = std::chrono::milliseconds;

    static const int32_t DefaultChunkSize = 16380;
    static const int32_t DefaultShrinkIntervalMs = 30000;

    mutable std::recursive_mutex lock;
    mutable std::condition_variable_any cv;

    std::string id;
    int32_t chunkSize;

    std::function<void(ByteArraySlice)> processor;

    mutable StateKind state = StateKind::Initialized;
    Logger log;

    mutable std::thread asyncProcessingThread;

    mutable Buffer::ByteArray data;
public:

    //region ctor/dtor

    ByteBufferAsyncProcessor(const std::string &id, int32_t chunkSize,
                             const std::function<void(ByteArraySlice)> &processor);

    ByteBufferAsyncProcessor(const std::string &id, const std::function<void(ByteArraySlice)> &processor);

    //endregion

    void cleanup0() const;

    bool terminate0(std::chrono::milliseconds timeout, StateKind stateToSet, const std::string &action) const;

    void ThreadProc() const;

    void start() const;

    bool stop(time_t timeout = time_t(0)) const;

    bool terminate(time_t timeout = time_t(0)/*InfiniteDuration*/) const;

    void put(Buffer::ByteArray const &newData, int32_t offset = 0);

    void put(Buffer::ByteArray const &newData, int32_t offset, int32_t count);
};


#endif //RD_CPP_BYTEBUFFERASYNCPROCESSOR_H
