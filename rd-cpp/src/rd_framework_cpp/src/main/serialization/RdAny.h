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
		using string = Wrapper<std::wstring>;
	}
	using InternedAny = mpark::variant<any::wrapped_super_t, any::string>;

	namespace any {
		template<typename T, typename Any>
		typename std::enable_if_t<!util::is_base_of_v<IPolymorphicSerializable, T>, any::string> get(Any const &any) {
			return mpark::get<any::string>(any);
		};

		template<typename T, typename Any>
		typename std::enable_if_t<util::is_base_of_v<IPolymorphicSerializable, T>, Wrapper<T>> get(Any &&any) {
			return Wrapper<T>::dynamic(mpark::get<wrapped_super_t>(std::forward<Any>(any)));
		};

		struct TransparentKeyEqual {
			using is_transparent = void;

			bool operator()(InternedAny const &val_l, InternedAny const &val_r) const {
				return val_l == val_r;
			}

			bool operator()(InternedAny const &val_l, wrapped_super_t const &val_r) const {
				return mpark::visit(util::make_visitor(
						[&](wrapped_super_t const &value) {
							return *value == *val_r;
						},
						[](any::string const &) {
							return false;
						}
				), val_l);
			}

			bool operator()(super_t const &val_l, InternedAny const &val_r) const {
				return operator()(val_r, val_l);
			}

			bool operator()(InternedAny const &val_l, super_t const &val_r) const {
				return mpark::visit(util::make_visitor(
						[&](wrapped_super_t const &value) {
							return *value == val_r;
						},
						[](any::string const &) {
							return false;
						}
				), val_l);
			}

			bool operator()(wrapped_super_t const &val_l, InternedAny const &val_r) const {
				return operator()(val_r, val_l);
			}

			bool operator()(InternedAny const &val_l, any::string const &val_r) const {
				return mpark::visit(util::make_visitor(
						[](wrapped_super_t const &value) {
							return false;
						},
						[&](any::string const &s) {
							return s == val_r;
						}
				), val_l);
			}

			bool operator()(any::string const &val_l, InternedAny const &val_r) const {
				return operator()(val_r, val_l);
			}
		};

		struct TransparentHash {
			using is_transparent = void;
			using transparent_key_equal = std::equal_to<>;

			size_t operator()(InternedAny const &value) const noexcept {
				return mpark::visit(util::make_visitor(
						[](wrapped_super_t const &value) {
							return std::hash<wrapped_super_t>()(value);
						},
						[](any::string const &value) {
							return std::hash<any::string>()(value);
						}
				), value);
			}

			size_t operator()(wrapped_super_t const &value) const noexcept {
				return std::hash<wrapped_super_t>()(value);
			}

			size_t operator()(super_t const &value) const noexcept {
				return std::hash<super_t>()(value);
			}

			size_t operator()(any::string const &value) const noexcept {
				return std::hash<any::string>()(value);
			}
		};
	}
}


#endif //RD_CPP_ANY_H
