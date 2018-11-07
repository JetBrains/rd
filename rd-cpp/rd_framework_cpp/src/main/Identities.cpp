//
// Created by jetbrains on 20.07.2018.
//

#include "Identities.h"

RdId Identities::next(const RdId &parent) const {
    RdId result = parent.mix(id_acc);
    id_acc += 2;
    return result;
}
