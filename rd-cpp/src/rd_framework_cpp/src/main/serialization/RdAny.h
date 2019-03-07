//
// Created by jetbrains on 3/7/2019.
//

#ifndef RD_CPP_ANY_H
#define RD_CPP_ANY_H

#include "ISerializable.h"
#include "core_util.h"
#include "wrapper.h"

#include "mpark/variant.hpp"

#include <variant>
#include <memory>
#include <string>
#include <cstring>

namespace rd {
	namespace any {
		using super_t = Wrapper<IPolymorphicSerializable>;
	}

	using RdAny = mpark::variant<any::super_t, std::wstring>;

	namespace any {
		template<typename T>
		typename std::enable_if<!std::is_base_of<IPolymorphicSerializable, T>::value, T>::type get(RdAny &&any) {
			return mpark::get<T>(std::move(any));
		};

		template<typename T>
		typename std::enable_if<std::is_base_of<IPolymorphicSerializable, T>::value, Wrapper<T>>::type get(RdAny &&any) {
			return Wrapper<T>::dynamic(mpark::get<super_t>(std::move(any)));
		};
	}
}


#endif //RD_CPP_ANY_H
