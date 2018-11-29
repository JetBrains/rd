//
// Created by operasfantom on 19.07.18.
//

#ifndef RD_CPP_CORE_LIFETIMEWRAPPER_H
#define RD_CPP_CORE_LIFETIMEWRAPPER_H


#include <memory>

#include "LifetimeImpl.h"

class Lifetime {
private:
    friend class LifetimeDefinition;

    std::shared_ptr<LifetimeImpl> ptr;
public:
    struct Hash {
        size_t operator()(Lifetime const &lw) const noexcept {
            return std::hash<std::shared_ptr<LifetimeImpl> >()(lw.ptr);
        }
    };

    static Lifetime const &Eternal();

    //region ctor/dtor

    Lifetime() = delete;

    Lifetime(Lifetime const &other) = default;

    Lifetime &operator=(Lifetime const &other) = default;

    Lifetime(Lifetime &&other) noexcept = default;

    Lifetime &operator=(Lifetime &&other) noexcept = default;

	~Lifetime() = default;
    //endregion

    friend bool operator==(Lifetime const &lw1, Lifetime const &lw2);

    explicit Lifetime(bool is_eternal = false);

    LifetimeImpl *operator->() const;

    Lifetime create_nested() const;

    template<typename T, typename F>
    static T use(F &&block) {
        Lifetime lw = Eternal().create_nested();
        T result = block(lw);
        lw->terminate();
        return result;
    }

    template<typename F>
    static void use(F &&block) {
        Lifetime lw = Eternal().create_nested();
        block(lw);
        lw->terminate();
    }
};


#endif //RD_CPP_CORE_LIFETIMEWRAPPER_H
