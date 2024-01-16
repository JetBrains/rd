#ifndef RD_CPP_FRAMEWORK_TRAITS_H
#define RD_CPP_FRAMEWORK_TRAITS_H

#include "serialization/Polymorphic.h"

#include <utility>
#include <type_traits>

namespace rd
{
namespace util
{
template <typename S, typename T = decltype((S::read(std::declval<rd::SerializationCtx&>(), std::declval<rd::Buffer&>())))>
using read_t = T;

static_assert(util::is_same_v<std::wstring, read_t<Polymorphic<std::wstring>>>, " ");

template <class T>
using is_bindable = std::is_base_of<IRdBindable, T>;

template <class T>
constexpr bool is_bindable_v = is_bindable<T>::value;

}	 // namespace util
}	 // namespace rd

#endif	  // RD_CPP_FRAMEWORK_TRAITS_H
