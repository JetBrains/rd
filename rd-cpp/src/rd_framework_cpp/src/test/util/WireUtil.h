//
// Created by jetbrains on 19.09.2018.
//

#ifndef RD_CPP_WIREUTIL_H
#define RD_CPP_WIREUTIL_H

#include "Host.h"

namespace rd {
    namespace util {
        uint16_t find_free_port();

        void sleep_this_thread(int ms);
    }
}

#endif //RD_CPP_WIREUTIL_H
