//
// Created by jetbrains on 09.07.2018.
//

#ifndef RD_CPP_CORE_INTERFACES_H
#define RD_CPP_CORE_INTERFACES_H


#include <functional>
#include <optional>

#include "SequentialLifetimes.h"
#include "Lifetime.h"

template<typename T>
class ISource {
public:
    virtual ~ISource() = default;

    virtual void advise(Lifetime lifetime, std::function<void(T const &)> handler) const = 0;
};

template<typename T>
class IViewable {
public:
    virtual ~IViewable() = default;

    virtual void view(Lifetime lifetime, std::function<void(Lifetime, T const &)> handler) const = 0;
};

template<typename T>
class ISignal : public ISource<T> {
public:
    virtual ~ISignal() = default;

    virtual void fire(T const &value) const = 0;
};

#endif //RD_CPP_CORE_INTERFACES_H
