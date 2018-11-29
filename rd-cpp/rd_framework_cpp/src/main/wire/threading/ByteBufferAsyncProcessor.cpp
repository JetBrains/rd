//
// Created by jetbrains on 15.09.2018.
//

#include "ByteBufferAsyncProcessor.h"

#include <demangle.h>

ByteBufferAsyncProcessor::ByteBufferAsyncProcessor(const std::string &id, int32_t chunkSize,
                                                   const std::function<void(ByteArraySlice)> &processor)
        : id(id), chunkSize(chunkSize), processor(processor) {}

ByteBufferAsyncProcessor::ByteBufferAsyncProcessor(const std::string &id,
                                                   const std::function<void(ByteArraySlice)> &processor)
        : ByteBufferAsyncProcessor(id, DefaultChunkSize, processor) {}

void ByteBufferAsyncProcessor::cleanup0() const {
    state = StateKind::Terminated;
}

bool ByteBufferAsyncProcessor::terminate0(time_t timeout,
                                          ByteBufferAsyncProcessor::StateKind stateToSet,
                                          const std::string &action) const {
    std::lock_guard<std::recursive_mutex> guard(lock);
    {
        if (state == StateKind::Initialized) {
            log.debug("Can't $action \'" + id + "\', because it hasn't been started yet");
            cleanup0();
            return true;
        }

        if (state >= stateToSet) {
            log.debug("Trying to $action async processor \'" + id + "\' but it's in state " +
                      std::to_string(static_cast<int>(state)));
            return true;
        }

        state = stateToSet;
        cv.notify_all();
    }

//    asyncProcessingThread.join(timeout);

    bool res = asyncProcessingThread.joinable();

    if (!res) {
        catch_([&]() {
//                asyncProcessingThread.stop();
        });
    }
    cleanup0();

    return res;
}

void ByteBufferAsyncProcessor::ThreadProc() const {
    while (true) {
        std::lock_guard<std::recursive_mutex> guard(lock);

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

        try {
            processor(ByteArraySlice(data, 0, data.size()));
            data.clear();
        } catch (std::exception const &e) {
            log.error("Exception while processing byte queue", &e);
            data.clear();
        }
    }
}

void ByteBufferAsyncProcessor::start() const {
    std::lock_guard<std::recursive_mutex> guard(lock);

    if (state != StateKind::Initialized) {
        log.debug("Trying to START async processor '$id' but it's in state '$state'");
        return;
    }

    state = StateKind::AsyncProcessing;

//    asyncProcessingThread = std::thread([this]() { this->ThreadProc(); });
/*    asyncProcessingThread.detach();*/
}

bool ByteBufferAsyncProcessor::stop(ByteBufferAsyncProcessor::time_t timeout) const {
    return terminate0(timeout, StateKind::Stopping, "STOP");
}

bool ByteBufferAsyncProcessor::terminate(ByteBufferAsyncProcessor::time_t timeout) const {
    return terminate0(timeout, StateKind::Terminating, "TERMINATE");
}

void ByteBufferAsyncProcessor::put(Buffer::ByteArray const &newData, int32_t offset) {
    put(newData, offset, newData.size());
}

void ByteBufferAsyncProcessor::put(Buffer::ByteArray const &newData, int32_t offset, int32_t count) {
    std::lock_guard<std::recursive_mutex> guard(lock);

    if (state >= StateKind::Stopping)
        return;

    assert(offset == 0);
    data.insert(data.end(), newData.begin(), newData.end());

    cv.notify_all();
}
