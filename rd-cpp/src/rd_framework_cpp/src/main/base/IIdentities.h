//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_IIDENTITIES_H
#define RD_CPP_IIDENTITIES_H

#include "RdId.h"

namespace rd {
    class IIdentities {
    public:
        virtual RdId next(const RdId &parent) const = 0;
    };
}


#endif //RD_CPP_IIDENTITIES_H
