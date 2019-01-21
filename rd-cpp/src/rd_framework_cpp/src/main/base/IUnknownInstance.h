//
// Created by jetbrains on 22.11.2018.
//

#ifndef RD_CPP_IUNKNOWNINSTANCE_H
#define RD_CPP_IUNKNOWNINSTANCE_H

#include "RdId.h"

class IUnknownInstance {
protected:
    RdId unknownId{0};

    IUnknownInstance();

    IUnknownInstance(const RdId &unknownId);

    IUnknownInstance(RdId &&unknownId);
};


#endif //RD_CPP_IUNKNOWNINSTANCE_H
