//
// Created by jetbrains on 25.07.2018.
//

#include "Protocol.h"

const Logger Protocol::initializationLogger;

IProtocol const *const Protocol::get_protocol() const {
    return this;
}
