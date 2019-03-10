//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASK_H
#define RD_CPP_RDTASK_H

#include "RdTaskResult.h"
#include "RdTaskImpl.h"
#include "Polymorphic.h"

#include <functional>


namespace rd {
	template<typename T, typename S = Polymorphic<T> >
	class RdTask final {
	private:
		using WT = value_or_wrapper<T>;

		using TRes = RdTaskResult<T, S>;

		mutable std::shared_ptr<RdTaskImpl<T, S> > ptr{std::make_shared<RdTaskImpl<T, S> >()};
	public:
		using result_type = RdTaskResult<T, S>;

		static RdTask<T, S> from_result(WT value) {
			RdTask<T, S> res;
			res.set(std::move(value));
			return res;
		}

		void set(WT value) const {
			typename TRes::Success t(std::move(value));
			ptr->result.set(tl::make_optional(std::move(t)));
		}

		void set_result(TRes value) const {
			ptr->result.set(tl::make_optional(std::move(value)));
		}

		void cancel() const {
			ptr->result.set(typename TRes::Cancelled());
		}

		void fault(std::exception const &e) const {
			ptr->result.set(typename TRes::Fault(e));
		}

		bool has_value() const {
			return ptr->result.get().has_value();
		}

		TRes value_or_throw() const {
			auto opt_res = std::move(ptr->result).steal();
			if (opt_res) {
				return *std::move(opt_res);
			}
			throw std::invalid_argument("task is empty");
		}

		bool isFaulted() const {
			return has_value() && value_or_throw().isFaulted(); //todo atomic
		}

		void advise(Lifetime lifetime, std::function<void(TRes const &)> handler) const {
			ptr->result.advise(lifetime,
							   [handler = std::move(handler)](tl::optional<TRes> const &opt_value) {
								   if (opt_value) {
									   handler(*opt_value);
								   }
							   });
		}
	};
}


#endif //RD_CPP_RDTASK_H
