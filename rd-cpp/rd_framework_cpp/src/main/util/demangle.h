//
// Created by jetbrains on 22.08.2018.
//

#ifndef RD_CPP_FRAMEWORK_UTIL_H
#define RD_CPP_FRAMEWORK_UTIL_H

#include "core_util.h"

#ifdef __GNUG__

#include <cxxabi.h>
#include <memory>
#include <cstdlib>

template<typename T>
std::string demangle() {
    int status = 0;
    const char *name = typeid(T).name();
    std::unique_ptr<char, decltype(&std::free)>
            real_name(abi::__cxa_demangle(name, nullptr, 0, &status), &std::free);
    MY_ASSERT_MSG((status == 0),
                  "getting real class name of:" + std::string(name) + "failed with status:" + std::to_string(status));
    return std::string(real_name.get());
}

#elif defined _MSC_VER

#include <cstring>

template<typename T>
std::string demangle()
{
    const char *name = typeid(T).name();
    const auto p = std::strchr(name, ' ');
    return p ? p + 1 : name;
}
#else
    template<typename T>
    std::string demangle() { return typeid(T).name(); }
#endif // __GNUG__


/*#include <mutex>
#include <functional>

template<typename R, typename F>
R synchronized(std::mutex &m, F block) {
    std::lock_guard<std::mutex> l(m);
    return F();
}*/

#endif //RD_CPP_FRAMEWORK_UTIL_H
