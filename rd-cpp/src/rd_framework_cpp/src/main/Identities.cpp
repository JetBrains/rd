//
// Created by jetbrains on 20.07.2018.
//

#include "Identities.h"

namespace rd {
    constexpr Identities::IdKind Identities::SERVER;
    constexpr Identities::IdKind Identities::CLIENT;

    Identities::Identities(IdKind dynamicKind) : id_acc(
            dynamicKind == IdKind::Client ? BASE_CLIENT_ID : BASE_SERVER_ID) {}

    RdId Identities::next(const RdId &parent) const {
        RdId result = parent.mix(id_acc);
        id_acc += 2;
        return result;
    }

    hash_t getPlatformIndependentHash(std::string const &that, hash_t initial) {
        for (auto c : that) {
            initial = initial * HASH_FACTOR + static_cast<hash_t>(c);
        }
        return initial;
    }

    hash_t getPlatformIndependentHash(int32_t const &that, hash_t initial) {
        return initial * HASH_FACTOR + (that + 1);
    }

    hash_t getPlatformIndependentHash(int64_t const &that, hash_t initial) {
        return initial * HASH_FACTOR + (that + 1);
    }
}
