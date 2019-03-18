//
// Created by jetbrains on 12.02.2019.
//

#ifndef RD_CPP_TRAITS_H
#define RD_CPP_TRAITS_H

#include "Void.h"

#include <type_traits>

namespace rd {
	namespace util {
		//region non_std

		template <typename T, typename U>
		constexpr bool is_same_v = std::is_same<T, U>::value;

		template <typename T, typename U>
		constexpr bool is_base_of_v = std::is_base_of<T, U>::value;
		//endregion

		template<typename T>
		/*inline */constexpr bool is_void = std::is_same<T, Void>::value;


		//region disjunction

		template<class...>
		struct disjunction : std::false_type {
		};
		template<class B1>
		struct disjunction<B1> : B1 {
		};
		template<class B1, class... Bn>
		struct disjunction<B1, Bn...>
				: std::conditional_t<bool(B1::value), B1, disjunction<Bn...>> {
		};
		//endregion


		//region in_heap

		template<typename T>
		using in_heap = util::disjunction<std::is_abstract<T>, std::is_same<T, std::wstring>>;
//		using in_heap = std::is_abstract<T>;
		template <typename T>
		/*inline */constexpr bool in_heap_v = in_heap<T>::value;

		static_assert(in_heap_v<std::wstring>, "std::wstring should be placed in shared memory");
		//endregion

		//region literal

		template<typename T>
		struct is_wstring_literal :
				std::is_same<
						T,
						std::add_lvalue_reference_t<const wchar_t[std::extent<std::remove_reference_t<T>>::value]>
				>
		{};

		template <typename T, bool = is_wstring_literal<typename std::remove_reference_t<T>>::value>
		struct not_string_literal {
			using type = std::wstring;
		};

		template <>
		struct not_string_literal<std::wstring, true> {
			using type = std::wstring;
		};

		template <typename T>
		struct not_string_literal<T, false> {
			using type = std::decay_t<T>;
		};

		static_assert(is_wstring_literal<decltype(L" ")>::value, "is_wstring trait doesn't work");
		static_assert(is_wstring_literal<decltype(L" ") &&>::value, "is_wstring trait doesn't work");
		static_assert(is_wstring_literal<decltype(L" ") &>::value, "is_wstring trait doesn't work");
//		static_assert(is_wstring_literal<std::wstring>::value, "is_wstring trait doesn't work");

		//endregion
	}
}

#endif //RD_CPP_TRAITS_H
