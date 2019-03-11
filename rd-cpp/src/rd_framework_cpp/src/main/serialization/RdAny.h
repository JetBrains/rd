//
// Created by jetbrains on 3/7/2019.
//

#ifndef RD_CPP_ANY_H
#define RD_CPP_ANY_H

#include "ISerializable.h"
#include "core_util.h"
#include "wrapper.h"

#include "mpark/variant.hpp"

#include <memory>
#include <string>
#include <cstring>

namespace rd {
	namespace any {
		using super_t = IPolymorphicSerializable;
		using wrapped_super_t = Wrapper<super_t>;
	}
	using RdAny = mpark::variant<any::wrapped_super_t, std::wstring>;

	namespace any {
		template<typename T, typename Any>
		typename std::enable_if<!std::is_base_of<IPolymorphicSerializable, T>::value, T>::type get(Any &&any) {
			return mpark::get<T>(std::forward<Any>(any));
		};

		template<typename T, typename Any>
		typename std::enable_if<std::is_base_of<IPolymorphicSerializable, T>::value, Wrapper < T>>

		::type get(Any &&any) {
			return Wrapper<T>::dynamic(mpark::get<wrapped_super_t>(std::forward<Any>(any)));
		};

		struct TransparentKeyEqual {
			using is_transparent = void;

			bool operator()(RdAny const &val_l, RdAny const &val_r) const {
				return val_l == val_r;
			}

			bool operator()(RdAny const &val_l, wrapped_super_t const &val_r) const {
				return mpark::visit(util::make_visitor(
						[&](wrapped_super_t const &value) {
							return *value == *val_r;
						},
						[](std::wstring const &) {
							return false;
						}
				), val_l);
			}

			bool operator()(super_t const &val_l, RdAny const &val_r) const {
				return operator()(val_r, val_l);
			}

			bool operator()(RdAny const &val_l, super_t const &val_r) const {
				return mpark::visit(util::make_visitor(
						[&](wrapped_super_t const &value) {
							return *value == val_r;
						},
						[](std::wstring const &) {
							return false;
						}
				), val_l);
			}

			bool operator()(wrapped_super_t const &val_l, RdAny const &val_r) const {
				return operator()(val_r, val_l);
			}

			bool operator()(RdAny const &val_l, std::wstring const &val_r) const {
				return mpark::visit(util::make_visitor(
						[](wrapped_super_t const &value) {
							return false;
						},
						[&](std::wstring const &s) {
							return s == val_r;
						}
				), val_l);
			}

			bool operator()(std::wstring const &val_l, RdAny const &val_r) const {
				return operator()(val_r, val_l);
			}
		};

		struct TransparentHash {
			using is_transparent = void;
			using transparent_key_equal = std::equal_to<>;

			size_t operator()(RdAny const &value) const noexcept {
//				std::wcerr << "HASH RdAny: " << mpark::get<std::wstring>(value) << " " << std::hash<RdAny>()(value) << std::endl;
//				return std::hash<RdAny>()(value);
				return mpark::visit(util::make_visitor(
						[](wrapped_super_t const &value) {
							return std::hash<wrapped_super_t>()(value);
						},
						[](std::wstring const &value) {
							return std::hash<std::wstring>()(value);
						}
				), value);
			}

			size_t operator()(wrapped_super_t const &value) const noexcept {
				return std::hash<wrapped_super_t>()(value);
			}

			size_t operator()(super_t const &value) const noexcept {
				return std::hash<super_t>()(value);
			}

			size_t operator()(std::wstring const &value) const noexcept {
//				std::wcerr << "HASH wstring: " << value << " " << std::hash<std::wstring>()(value) << std::endl;
				return std::hash<std::wstring>()(value);
			}
		};
	}
}


#endif //RD_CPP_ANY_H
