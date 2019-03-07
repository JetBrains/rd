//
// Created by jetbrains on 3/7/2019.
//

#ifndef RD_CPP_ANY_H
#define RD_CPP_ANY_H

#include "ISerializable.h"

#include <variant>
#include <memory>
#include <string>
#include <cstring>

namespace rd {
	using RdAny = std::variant<std::unique_ptr<IPolymorphicSerializable>, std::wstring>;
}


#endif //RD_CPP_ANY_H
