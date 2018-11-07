//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASKRESULT_H
#define RD_CPP_RDTASKRESULT_H


#include <exception>
#include <variant>

#include "Polymorphic.h"
#include "overloaded.h"

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

    class Fault {
        /*std::exception error;
    public:
        explicit Fault(const std::exception &error) : error(error) {};*/
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
                //todo read reason
                return Fault();
            }
            default:
                throw std::invalid_argument("Fail on RdTaskResult reading with kind: " + to_string(kind));
        }
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        std::visit(overloaded{
                [&ctx, &buffer](typename RdTaskResult::Success const &value) {
                    buffer.write_pod<int32_t>(0);
                    S::write(ctx, buffer, value.value);
                },
                [&buffer](typename RdTaskResult::Cancelled const &value) {
                    buffer.write_pod<int32_t>(1);
                },
                [&buffer](typename RdTaskResult::Fault const &value) {
                    buffer.write_pod<int32_t>(2);
                    //todo write reason
                },
        }, v);
    }

    T unwrap() {
        return std::visit(overloaded{
                [](typename RdTaskResult::Success const &value) -> T {
                    return value.value;
                },
                [](typename RdTaskResult::Cancelled const &value) -> T {
                    throw std::invalid_argument("Task finished in Cancelled state");
                },
                [](typename RdTaskResult::Fault const &value) -> T {
//                    throw value.error;
                    throw std::exception();
                },
        }, v);
    }

    friend bool operator==(const RdTaskResult &lhs, const RdTaskResult &rhs) {
        return &lhs == &rhs;
    }

    friend bool operator!=(const RdTaskResult &lhs, const RdTaskResult &rhs) {
        return !(rhs == lhs);
    }

private:
    std::variant<Success, Cancelled, Fault> v;
};


#endif //RD_CPP_RDTASKRESULT_H
