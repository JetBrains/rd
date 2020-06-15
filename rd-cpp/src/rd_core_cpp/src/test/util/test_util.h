#ifndef RD_CPP_CORE_TEST_UTIL_H
#define RD_CPP_CORE_TEST_UTIL_H

#include "reactive/ViewableList.h"
#include "reactive/ViewableSet.h"
#include "reactive/ViewableMap.h"

#include "util/core_util.h"

#include <vector>

namespace rd
{
namespace test
{
namespace util
{
using namespace rd::util;
using namespace std::string_literals;

template <typename T0, typename... T>
constexpr std::vector<T0> arrayListOf(T0&& arg, T&&... args)
{
	return std::vector<T0>{std::forward<T0>(arg), std::forward<T>(args)...};
}
}	 // namespace util
}	 // namespace test
}	 // namespace rd

#endif	  // RD_CPP_CORE_TEST_UTIL_H
