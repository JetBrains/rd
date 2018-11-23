//
// Created by jetbrains on 20.07.2018.
//

#include "Identities.h"

Identities::IdKind Identities::SERVER = Identities::IdKind::Server;
Identities::IdKind Identities::CLIENT = Identities::IdKind::Client;

Identities::Identities(Identities::IdKind dynamicKind) : id_acc(
        dynamicKind == IdKind::Client ? BASE_CLIENT_ID : BASE_SERVER_ID) {}

RdId Identities::next(const RdId &parent) const {
    RdId result = parent.mix(id_acc);
    id_acc += 2;
    return result;
}
