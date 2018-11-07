//
// Created by jetbrains on 09.07.2018.
//

#ifndef RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H
#define RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H

#include "LifetimeDefinition.h"
#include "Lifetime.h"

class SequentialLifetimes {
private:
    std::shared_ptr<LifetimeDefinition> current_def = LifetimeDefinition::get_shared_eternal();
    Lifetime parent_lifetime;
public:
    SequentialLifetimes() = delete;

    explicit SequentialLifetimes(Lifetime parent_lifetime);

    Lifetime next();

    void terminate_current();

    bool is_terminated();

    void set_current_lifetime(std::shared_ptr<LifetimeDefinition> new_def);
};


#endif //RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H
