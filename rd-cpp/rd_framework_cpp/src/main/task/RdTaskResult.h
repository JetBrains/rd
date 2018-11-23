//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASKRESULT_H
#define RD_CPP_RDTASKRESULT_H

#include "Polymorphic.h"
#include "overloaded.h"
#include "mpark/variant.hpp"

#include <exception>

template<typename T, typename S = Polymorphic<T> >
class RdTaskResult : ISerializable {
public:
    class Success {
    public:
        T value;

        explicit Success(T value) : value(std::move(value)) {}
    };

    class Cancelled {

    };

    class Fault {//todo
    public:
        std::wstring reasonTypeFqn;
        std::wstring reasonMessage;
        std::wstring reasonAsText;

        Fault(std::wstring reasonTypeFqn, std::wstring reasonMessage, std::wstring reasonAsText) :
                reasonTypeFqn(std::move(reasonTypeFqn)),
                reasonMessage(std::move(reasonMessage)),
                reasonAsText(std::move(reasonAsText)) {}

    public:
        explicit Fault(const std::exception &e) {
//todo
            //            reasonMessage = e.what();
        };
    };

    RdTaskResult(Success &&v) : v(std::move(v)) {}

    RdTaskResult(Cancelled &&v) : v(std::move(v)) {}

    RdTaskResult(Fault &&v) : v(std::move(v)) {}

    virtual ~RdTaskResult() = default;

public:
    static RdTaskResult<T, S> read(SerializationCtx const &ctx, Buffer const &buffer) {
        int32_t kind = buffer.read_pod<int32_t>();
        switch (kind) {
            case 0: {
                return Success(S::read(ctx, buffer));
            }
            case 1: {
                return Cancelled();
            }
            case 2: {
                auto reasonTypeFqn = buffer.readWString();
                auto reasonMessage = buffer.readWString();
                auto reasonAsText = buffer.readWString();
                return Fault(std::move(reasonTypeFqn), std::move(reasonMessage), std::move(reasonAsText));
            }
            default:
                throw std::invalid_argument("Fail on RdTaskResult reading with kind: " + to_string(kind));
        }
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        mpark::visit(make_visitor(
                [&ctx, &buffer](typename RdTaskResult::Success const &value) {
                    buffer.write_pod<int32_t>(0);
                    S::write(ctx, buffer, value.value);
                },
                [&buffer](typename RdTaskResult::Cancelled const &value) {
                    buffer.write_pod<int32_t>(1);
                },
                [&buffer](typename RdTaskResult::Fault const &value) {
                    buffer.write_pod<int32_t>(2);
                    buffer.writeWString(value.reasonTypeFqn);
                    buffer.writeWString(value.reasonMessage);
                    buffer.writeWString(value.reasonAsText);
                }
        ), v);
    }

    T unwrap() const {
        return mpark::visit(make_visitor(
                [](typename RdTaskResult::Success const &value) -> T {
                    return value.value;
                },
                [](typename RdTaskResult::Cancelled const &value) -> T {
                    throw std::invalid_argument("Task finished in Cancelled state");
                },
                [](typename RdTaskResult::Fault const &value) -> T {
//                    throw value.error;
                    throw std::exception();
                }
        ), v);
    }

    bool isFaulted() const {
        return v.index() == 2;
    }

    friend bool operator==(const RdTaskResult &lhs, const RdTaskResult &rhs) {
        return &lhs == &rhs;
    }

    friend bool operator!=(const RdTaskResult &lhs, const RdTaskResult &rhs) {
        return !(rhs == lhs);
    }

private:
    mpark::variant<Success, Cancelled, Fault> v;
};


#endif //RD_CPP_RDTASKRESULT_H
