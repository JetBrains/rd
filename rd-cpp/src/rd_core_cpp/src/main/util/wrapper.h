//
// Created by jetbrains on 05.02.2019.
//

#ifndef RD_CPP_WRAPPER_H
#define RD_CPP_WRAPPER_H

#include "traits.h"

#include "optional.hpp"

#include <type_traits>
#include <memory>
#include <utility>

namespace rd {
	template<typename T>
	class Wrapper;

	template<typename T, typename R = void>
	struct helper {
		using value_or_wrapper_type = T;
		using opt_or_wrapper_type = tl::optional<T>;
		using property_storage = tl::optional<T>;
		using raw_type = T;
	};

	template<typename T>
	struct helper<T, typename std::enable_if_t<util::in_heap_v<T>>> {
		using value_or_wrapper_type = Wrapper<T>;
		using opt_or_wrapper_type = Wrapper<T>;
		using property_storage = Wrapper<T>;
		using raw_type = T;
	};

	template<typename T>
	struct helper<Wrapper<T>> {
		using value_or_wrapper_type = Wrapper<T>;
		using opt_or_wrapper_type = Wrapper<T>;
		using property_storage = Wrapper<Wrapper<T>>;
		using raw_type = T;
	};

	template<typename T>
	using value_or_wrapper = typename helper<T>::value_or_wrapper_type;

	template<typename T>
	using opt_or_wrapper = typename helper<T>::opt_or_wrapper_type;

	template<typename T>
	using property_storage = typename helper<T>::property_storage;

	template<typename T>
	using raw_type = typename helper<T>::raw_type;

	template<typename T>
	class Wrapper : private std::shared_ptr<T> {
	private:
		template<typename>
		friend
		class Wrapper;

		using Base = std::shared_ptr<T>;
		//		std::unique_ptr<T> ptr;
	public:
		//region ctor/dtor

		Wrapper() = default;

		Wrapper(Wrapper const &) = default;

		Wrapper &operator=(Wrapper const &) = default;

		Wrapper(Wrapper &&) noexcept = default;

		Wrapper &operator=(Wrapper &&) noexcept = default;

		explicit constexpr Wrapper(std::nullptr_t) noexcept {}

		template<typename R, typename = typename std::enable_if_t<util::is_base_of_v<T, R>>>
		Wrapper(Wrapper<R> const &other) : Wrapper(std::static_pointer_cast<T>(static_cast<std::shared_ptr<R>>(other))) {}

		template<typename R, typename = typename std::enable_if_t<util::is_base_of_v<T, R>>>
		Wrapper(Wrapper<R> &&other) : Wrapper(std::static_pointer_cast<T>(std::move(other))) {}

		template<typename F/*, typename = typename std::enable_if<!util::in_heap_v<std::decay_t<F>>>::type*/>
		Wrapper(F &&value) {
//			Base::operator=(std::make_shared<typename util::not_string_literal<F&&>::type >(std::forward<F>(value)));
		}

		template<typename F>
		Wrapper(std::shared_ptr<F> const &ptr) noexcept : Base(std::static_pointer_cast<T>(ptr)) {}

		template<typename F>
		Wrapper(std::shared_ptr<F> &&ptr) noexcept : Base(std::static_pointer_cast<T>(std::move(ptr))) {}

		template<typename U = T, typename R = typename std::enable_if_t<!util::in_heap_v<std::decay_t<U>>>>
		Wrapper(tl::optional<T> &&opt) {
			if (opt) {
				*this = std::make_shared<T>(*std::move(opt));
			}
		}

		template<typename R>
		static Wrapper<T> dynamic(Wrapper<R> const &w) {
			return Wrapper<T>(std::dynamic_pointer_cast<T>(w));
		}

		template<typename R>
		static Wrapper<T> dynamic(Wrapper<R> &&w) {
			return Wrapper<T>(std::dynamic_pointer_cast<T>(std::move(w)));
		}

		//endregion

		constexpr T &operator*() &{
			return *static_cast<Base &>(*this);
		};

		constexpr T const &operator*() const &{
			return *static_cast<Base const &>(*this);
		};

		/*constexpr T &&operator*() &&{
			return *ptr.get();
		};*/

		T const *operator->() const {
			return Base::operator->();
		}

		T *operator->() {
			return Base::operator->();
		}

		constexpr explicit operator bool() const noexcept {
			return Base::operator bool();
		}

		friend bool operator==(const Wrapper &lhs, const Wrapper &rhs) {
			bool is_lhs = (bool)lhs;
			bool is_rhs = (bool)rhs;
			if (is_lhs != is_rhs) {
				return false;
			}
			if (!is_lhs && !is_rhs) {
				return true;
			}
			return *lhs.get() == *rhs.get();
		}

		friend bool operator!=(const Wrapper &lhs, const Wrapper &rhs) {
			return !(rhs == lhs);
		}
	};

	namespace wrapper {
		template<typename T>
		decltype(auto) get(T &&w) {
			return std::forward<T>(w);
		}

		template<typename T>
		decltype(auto) get(T const &w) {
			return w;
		}

		template<typename T>
		T &get(Wrapper<T> &w) {
			return *w;
		}

		template<typename T>
		T const &get(Wrapper<T> const &w) {
			return *w;
		}

		template<typename T>
		T &&get(Wrapper<T> &&w) {
			return *std::move(std::move(w));
		}

		/*template<typename T>
		Wrapper<T> make_wrapper(std::unique_ptr<T> &&value) {
			return Wrapper<T>(std::move(value));
		}*/

		template<typename T>
		typename std::enable_if_t<!util::in_heap_v<T>, T> unwrap(Wrapper<T> &&ptr) {
			return std::move(*ptr);
		}

		template<typename T>
		typename std::enable_if_t<util::in_heap_v<T>, Wrapper<T>> unwrap(Wrapper<T> &&ptr) {
			return Wrapper<T>(std::move(ptr));
		}

		template<typename T, typename ...Args>
		Wrapper<T> make_wrapper(Args &&... args) {
			return Wrapper<T>(std::make_shared<T>(std::forward<Args>(args)...));
		}

		/*template<typename T>
		constexpr Wrapper<T> null_wrapper = Wrapper<T>(nullptr);*/
	}
}

namespace std {
	template<typename T>
	struct hash<rd::Wrapper<T>> {
		size_t operator()(const rd::Wrapper<T> &value) const noexcept {
			return std::hash<T>()(*value);
		}
	};
}
#endif //RD_CPP_WRAPPER_H
