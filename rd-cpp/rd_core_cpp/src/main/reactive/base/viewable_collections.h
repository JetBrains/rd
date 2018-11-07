//
// Created by jetbrains on 22.09.2018.
//

#ifndef RD_CPP_VIEWABLE_COLLECTIONS_H
#define RD_CPP_VIEWABLE_COLLECTIONS_H

#include <string>

enum class AddRemove {
    ADD, REMOVE
};

std::string to_string(AddRemove kind);

enum class Op {
    ADD, UPDATE, REMOVE, ACK
};

std::string to_string(Op op);

#endif //RD_CPP_VIEWABLE_COLLECTIONS_H
