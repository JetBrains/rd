//
// Created by jetbrains on 09.07.2018.
//

#ifndef RD_CPP_CORE_LIFETIME_DEFINITION_H
#define RD_CPP_CORE_LIFETIME_DEFINITION_H


#include "LifetimeImpl.h"
#include "Lifetime.h"

#include <functional>

class LifetimeDefinition {
private:
    friend class SequentialLifetimes;

    bool eternaled = false;
public:
    Lifetime lifetime;

    LifetimeDefinition() = delete;

    explicit LifetimeDefinition(bool is_eternal = false);

    explicit LifetimeDefinition(const Lifetime &parent);

    LifetimeDefinition(LifetimeDefinition const &other) = delete;

    LifetimeDefinition &operator=(LifetimeDefinition const &other) = delete;

    LifetimeDefinition(LifetimeDefinition &&other) noexcept = default;

    LifetimeDefinition &operator=(LifetimeDefinition &&other) noexcept = default;

//    static std::shared_ptr<LifetimeDefinition> eternal;
    static std::shared_ptr<LifetimeDefinition> get_shared_eternal();

    bool is_terminated() const;

    bool is_eternal() const;

    void terminate();
};

#endif //RD_CPP_CORE_LIFETIME_DEFINITION_H
