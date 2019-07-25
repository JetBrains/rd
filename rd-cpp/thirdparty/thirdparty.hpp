#ifndef RD_CPP_THIRDPARTY_HPP
#define RD_CPP_THIRDPARTY_HPP

#include "tsl/ordered_set.h"
#include "tsl/ordered_map.h"

//region memoory
/*#include <foonathan/memory/container.hpp>
#include <foonathan/memory/memory_pool.hpp>
#include <foonathan/memory/smart_ptr.hpp>
#include <foonathan/memory/static_allocator.hpp>
#include <foonathan/memory/temporary_allocator.hpp>
#include <foonathan/memory/joint_allocator.hpp>

#include <foonathan/memory/namespace_alias.hpp>*/
//endregion

#if __cplusplus >= 201703L

#include <optional>
#include <variant>
#include <string_view>

namespace rd {
	using std::optional;
	using std::make_optional;
	using std::nullopt_t;
	using std::nullopt;
	using std::variant;
	using std::get;
	using std::visit;
	using std::string_view;
	using std::wstring_view;
	using namespace std::literals;
}

#else

#pragma warning(push)
#pragma warning(disable:4583)
#pragma warning(disable:4582)
#include "optional.hpp"
#include "mpark/variant.hpp"
#include "nonstd/string_view.hpp"
#pragma warning(pop)

namespace rd {
	using tl::optional;
	using tl::make_optional;
	using tl::nullopt_t;
	using tl::nullopt;
	using mpark::variant;
	using mpark::get;
	using mpark::visit;
	using nonstd::string_view;
	using nonstd::wstring_view;
	using namespace std::literals;
	using namespace nonstd::literals;
}

#endif


namespace rd {
	using tsl::ordered_set;
	using tsl::ordered_map;
}

#endif //RD_CPP_THIRDPARTY_H
