//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_IDENTITIES_H
#define RD_CPP_FRAMEWORK_IDENTITIES_H


#include <string>
#include <memory>
#include <iostream>

#include "IIdentities.h"
#include "interfaces.h"
#include "RdId.h"

enum class IdKind {
    Client,
    Server
};

using hash_t = int64_t;

const hash_t DEFAULT_HASH = 19;
const hash_t HASH_FACTOR = 31;

//PLEASE DO NOT CHANGE IT!!! IT'S EXACTLY THE SAME ON C# SIDE
template<typename T>
inline hash_t getPlatformIndependentHash(T const &that, hash_t initial = DEFAULT_HASH);

template<>
inline hash_t getPlatformIndependentHash<std::string>(std::string const &that, hash_t initial) {
//    std::cerr << that << " " << initial << std::endl;
    for (auto c : that) {
        initial = initial * HASH_FACTOR + static_cast<hash_t>(c);
    }
//    std::cerr << initial << std::endl;
    return initial;
}

template<>
inline hash_t getPlatformIndependentHash<int32_t>(int32_t const &that, hash_t initial) {
    return initial * HASH_FACTOR + (that + 1);
}

template<>
inline hash_t getPlatformIndependentHash<int64_t>(int64_t const &that, hash_t initial) {
    return initial * HASH_FACTOR + (that + 1);
}

class Identities : public IIdentities {
private:
    mutable int32_t id_acc;
public:
    static const int32_t BASE_CLIENT_ID = RdId::MAX_STATIC_ID;

    static const int32_t BASE_SERVER_ID = RdId::MAX_STATIC_ID + 1;

    //region ctor/dtor

    explicit Identities(IdKind dynamicKind = IdKind::Client) : id_acc(
            dynamicKind == IdKind::Client ? BASE_CLIENT_ID : BASE_SERVER_ID) {}

    virtual ~Identities() = default;
    //endregion

    RdId next(const RdId &parent) const override;
};


#endif //RD_CPP_FRAMEWORK_IDENTITIES_H
