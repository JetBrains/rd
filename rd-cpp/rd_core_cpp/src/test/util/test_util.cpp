//
// Created by jetbrains on 16.07.2018.
//

#include <string>

#include "test_util.h"

std::string operator "" _s(char const *str, size_t len) {
    return std::string(str, len);
}