//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASKRESULT_H
#define RD_CPP_RDTASKRESULT_H

#include "Polymorphic.h"
#include "overloaded.h"
#include "wrapper.h"

#include "mpark/variant.hpp"

#include <exception>

namespace rd {
	template<typename T, typename S = Polymorphic<T> >
	class RdTaskResult : public ISerializable {
		using WT = value_or_wrapper<T>;
	public:
		class Success {
		public:
			mutable WT value;

			explicit Success(WT &&value) : value(std::move(value)) {}
		};

		class Cancelled {

		};

		class Fault {
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

		//region ctor/dtor

		RdTaskResult(Success &&v) : v(std::move(v)) {}

		RdTaskResult(Cancelled &&v) : v(std::move(v)) {}

		RdTaskResult(Fault &&v) : v(std::move(v)) {}


		RdTaskResult(RdTaskResult const &) = default;

		RdTaskResult(RdTaskResult &&) = default;

		RdTaskResult &operator=(RdTaskResult &&) = default;

		virtual ~RdTaskResult() = default;
		//endregion

		static RdTaskResult<T, S> read(SerializationCtx const &ctx, Buffer const &buffer) {
			int32_t kind = buffer.read_pod<int32_t>();
			switch (kind) {
			case 0: {
				return Success(std::move(S::read(ctx, buffer)));
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
				throw std::invalid_argument("Fail on RdTaskResult reading with kind: " + std::to_string(kind));
			}
		}

		void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
			mpark::visit(util::make_visitor(
				             [&ctx, &buffer](Success const &value) {
					             buffer.write_pod<int32_t>(0);
					             S::write(ctx, buffer, value.value);
				             },
				             [&buffer](Cancelled const &value) {
					             buffer.write_pod<int32_t>(1);
				             },
				             [&buffer](Fault const &value) {
					             buffer.write_pod<int32_t>(2);
					             buffer.writeWString(value.reasonTypeFqn);
					             buffer.writeWString(value.reasonMessage);
					             buffer.writeWString(value.reasonAsText);
				             }
			             ), v);
		}

		WT unwrap() const {
			return mpark::visit(util::make_visitor(
				                    [](Success const &value) -> WT {
					                    return value.value;
				                    },
				                    [](Cancelled const &value) -> WT {
					                    throw std::invalid_argument("Task finished in Cancelled state");
				                    },
				                    [](Fault const &value) -> WT {
					                    //                    throw value.error;
					                    throw std::runtime_error(to_string(value.reasonMessage));
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
		mutable mpark::variant<Success, Cancelled, Fault> v;
	};
}


#endif //RD_CPP_RDTASKRESULT_H
