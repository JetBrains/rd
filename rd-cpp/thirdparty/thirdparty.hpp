//
// Created by jetbrains on 4/7/2019.
//

#ifndef RD_CPP_THIRDPARTY_HPP
#define RD_CPP_THIRDPARTY_HPP

#include "tsl/ordered_set.h"
#include "tsl/ordered_map.h"

#if __cplusplus >= 201703L

#include <optional>
#include <variant>
#include <string_view>

namespace rd {
	using std::optional;
	using std::nullopt_t;
	using std::nullopt;
	using std::variant;
	using std::get;
	using std::visit;
	using std::string_view;
}

#else

#include "optional.hpp"
#include "mpark/variant.hpp"
#include "nonstd/string_view.hpp"

namespace rd {
	using tl::optional;
	using tl::nullopt_t;
	using tl::nullopt;
	using mpark::variant;
	using mpark::get;
	using mpark::visit;
	using nonstd::string_view;
}

#endif


namespace rd {
	using tsl::ordered_set;
	using tsl::ordered_map;
}

#endif //RD_CPP_THIRDPARTY_H
