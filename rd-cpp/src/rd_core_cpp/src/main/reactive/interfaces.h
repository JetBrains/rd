//
// Created by jetbrains on 09.07.2018.
//

#ifndef RD_CPP_CORE_INTERFACES_H
#define RD_CPP_CORE_INTERFACES_H

#include "Lifetime.h"
#include "wrapper.h"
#include "traits.h"

#include <functional>
#include <type_traits>

namespace rd {
	template<typename T>
	class ISource {
	public:
		virtual ~ISource() = default;

		virtual void advise(Lifetime lifetime, std::function<void(T const &)> handler) const = 0;

		template<typename F>
		void advise_eternal(F &&handler) const {
			advise(Lifetime::Eternal(), std::forward<F>(handler));
		}

		void advise(Lifetime lifetime, std::function<void()> handler) const {
			advise(lifetime, [handler = std::move(handler)](Void) {
				handler();
			});
		}
	};

	template<typename T>
	class IViewable {
	public:
		virtual ~IViewable() = default;

		virtual void view(Lifetime lifetime, std::function<void(Lifetime, T const &)> handler) const = 0;
	};

	template<typename T>
	class ISignal : public ISource<T> {
	protected:
		using WT = value_or_wrapper<T>;
	public:
		virtual ~ISignal() = default;

		virtual void fire(T const &value) const = 0;
		
		template<typename U = T>
		typename std::enable_if<std::is_same<U, Void>::value>::type fire() const {
			fire(Void{});
		}
	};
}

#endif //RD_CPP_CORE_INTERFACES_H
