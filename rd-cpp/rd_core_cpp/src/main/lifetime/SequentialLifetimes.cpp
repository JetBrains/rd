//
// Created by jetbrains on 09.07.2018.
//

#include "SequentialLifetimes.h"

SequentialLifetimes::SequentialLifetimes(Lifetime parent_lifetime) : parent_lifetime(
        parent_lifetime) {
    parent_lifetime->add_action([this]() {
        set_current_lifetime(LifetimeDefinition::get_shared_eternal());
    });
}

Lifetime SequentialLifetimes::next() {
    std::shared_ptr<LifetimeDefinition> new_def(new LifetimeDefinition(parent_lifetime));
    set_current_lifetime(new_def);
    return current_def->lifetime;
}

void SequentialLifetimes::terminate_current() {
    set_current_lifetime(LifetimeDefinition::get_shared_eternal());
}

bool SequentialLifetimes::is_terminated() {
    return current_def->is_eternal() || current_def->is_terminated();
}

void SequentialLifetimes::set_current_lifetime(std::shared_ptr<LifetimeDefinition> new_def) {
    std::shared_ptr<LifetimeDefinition> prev = current_def;
    current_def = new_def;
    prev->terminate();
}
