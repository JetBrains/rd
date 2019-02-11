//
// Created by jetbrains on 09.07.2018.
//

#include "LifetimeDefinition.h"

namespace rd {
    LifetimeDefinition::LifetimeDefinition(bool eternaled) : eternaled(eternaled), lifetime(eternaled) {}

    LifetimeDefinition::LifetimeDefinition(const Lifetime &parent) : LifetimeDefinition(false) {
        parent->attach_nested(lifetime.ptr);
    }

    bool LifetimeDefinition::is_terminated() const {
        return lifetime->is_terminated();
    }

    void LifetimeDefinition::terminate() {
        lifetime->terminate();
    }

    bool LifetimeDefinition::is_eternal() const {
        return lifetime->is_eternal();
    }

    namespace {
        LifetimeDefinition ETERNAL(true);
    }

    std::shared_ptr<LifetimeDefinition> LifetimeDefinition::get_shared_eternal() {
        return std::shared_ptr<LifetimeDefinition>(&ETERNAL, [](LifetimeDefinition *ld) {});
    }

//std::shared_ptr<LifetimeDefinition> LifetimeDefinition::eternal(new LifetimeDefinition(true));
}
