//
// Created by jetbrains on 23.07.2018.
//


#include "SignalX.h"
#include "RdBindableBase.h"

bool RdBindableBase::is_bound() const {
    return parent != nullptr;
}

void RdBindableBase::bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const {
    MY_ASSERT_MSG((this->parent == nullptr), ("Trying to bound already bound this to " + parent->location.toString()));
    lf->bracket([this, lf, parent, name]() {
                    this->parent = parent;
                    location = parent->location.sub(name, ".");
                    this->bind_lifetime = lf;
                },
                [this, lf]() {
                    this->bind_lifetime = lf;
                    location = location.sub("<<unbound>>", "::");
                    this->parent = nullptr;
                    rdid = RdId::Null();
                }
    );

    get_protocol()->scheduler->assert_thread();

    priorityAdviseSection(
            [this, lf]() mutable { init(lf); }
    );
}

void RdBindableBase::identify(const IIdentities &identities, RdId const &id) const {
    MY_ASSERT_MSG(rdid.isNull(), "Already has RdId: " + rdid.toString() + ", entity: $this");
    MY_ASSERT_MSG(!id.isNull(), "Assigned RdId mustn't be null, entity: $this");

    this->rdid = id;
    //for (const auto&[name, child] : bindableChildren) {
    for (const auto &it : bindableChildren) {
        identifyPolymorphic(*(it.second), identities, id.mix("." + it.first));
    }
    /*for (const auto &it : bindable_extensions) {
        identifyPolymorphic(*(it.second), identities, id.mix("." + it.first));
    }*/
}

const IProtocol *const RdBindableBase::get_protocol() const {
    if (parent && parent->get_protocol()) {
        return parent->get_protocol();
    } else {
        throw std::invalid_argument("Not bound");
    }
}

SerializationCtx const &RdBindableBase::get_serialization_context() const {
    if (parent) {
        return parent->get_serialization_context();
    } else {
        throw std::invalid_argument("Not bound");
    }
}

void RdBindableBase::init(Lifetime lifetime) const {
    //for (const auto&[name, child] : bindableChildren) {
    for (const auto &it : bindableChildren) {
        if (it.second != nullptr) {
            bindPolymorphic(*(it.second), lifetime, this, it.first);
        }
    }
    /*for (const auto &it : bindable_extensions) {
        if (it.second != nullptr) {
            bindPolymorphic(*(it.second), lifetime, this, it.first);
        }
    }*/
}


