//
// Created by jetbrains on 13.08.2018.
//

#ifndef RD_CORE_CPP_UTIL_H
#define RD_CORE_CPP_UTIL_H

#include "optional.hpp"

#include <type_traits>
#include <memory>
#include <cassert>
#include <iostream>
#include <string>

#define MY_ASSERT_MSG(expr, msg) if(!(expr)){std::cerr<<std::endl<<(msg)<<std::endl;assert(expr);}
#define MY_ASSERT_THROW_MSG(expr, msg) if(!(expr)){std::cerr<<std::endl<<(msg)<<std::endl;throw std::runtime_error(msg);}

template<typename T>
struct KeyEqualSmartPtr {
    using is_transparent = void;

    bool operator()(std::unique_ptr<T> const &ptr_l, std::unique_ptr<T> const &ptr_r) const {
        return *ptr_l == *ptr_r;
    }

    bool operator()(T const &val_r, std::unique_ptr<T> const &ptr_l) const {
        return *ptr_l == val_r;
    }
};

template<typename T>
struct HashSmartPtr {
    using is_transparent = void;

    size_t operator()(std::unique_ptr<T> const &ptr) const noexcept {
        return std::hash<T>()(*ptr);
    }

    size_t operator()(T const &val) const noexcept {
        return std::hash<T>()(val);
    }
};

template<typename T>
typename std::enable_if_t<std::is_arithmetic_v<T>, std::string> to_string(T const &val) {
    return std::to_string(val);
}

template<typename T>
typename std::enable_if_t<!std::is_arithmetic_v<T>, std::string> to_string(T const &val) {
    return "";
}

template<>
inline std::string to_string<std::string>(std::string const &val) {
    return val;
}

template<typename T>
inline std::string to_string(tl::optional<T> const &val) {
    if (val.has_value()) {
        return to_string(*val);
    } else {
        return "nullopt";
    }
}

//todo

#endif //RD_CORE_CPP_UTIL_H
