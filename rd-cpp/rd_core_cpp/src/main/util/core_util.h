//
// Created by jetbrains on 13.08.2018.
//

#ifndef RD_CORE_CPP_UTIL_H
#define RD_CORE_CPP_UTIL_H

#include <type_traits>
#include <memory>
#include <cassert>
#include <iostream>
#include <string>

#define MY_ASSERT_MSG(expr, msg) if(!(expr)){std::cerr<<std::endl<<(msg)<<std::endl;assert(expr);}
#define MY_ASSERT_THROW_MSG(expr, msg) if(!(expr)){std::cerr<<std::endl<<(msg)<<std::endl;throw std::runtime_error(msg);}

template<typename T>
struct KeyEqualSharedPtr {
    using is_transparent = void;

    bool operator()(std::shared_ptr<T> const &ptr_l, std::shared_ptr<T> const &ptr_r) const {
        return *ptr_l == *ptr_r;
    }

    bool operator()(T const &val_r, std::shared_ptr<T> const &ptr_l) const {
        return *ptr_l == val_r;
    }
};

template<typename T>
struct HashSharedPtr {
    using is_transparent = void;

    size_t operator()(std::shared_ptr<T> const &ptr) const noexcept {
        return std::hash<T>()(*ptr);
    }

    size_t operator()(T const &val) const noexcept {
        return std::hash<T>()(val);
    }
};

template<typename U>
std::shared_ptr<U> deleted_shared_ptr(U &element) {
    return std::shared_ptr<U>(&element, [](U *) {});
}

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

//todo

#endif //RD_CORE_CPP_UTIL_H
