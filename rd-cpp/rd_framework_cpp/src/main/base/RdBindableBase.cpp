//
// Created by jetbrains on 23.07.2018.
//


#include "SignalX.h"
#include "RdBindableBase.h"

bool RdBindableBase::is_bound() const {
    return parent != nullptr;
}

void RdBindableBase::bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const {
    MY_ASSERT_MSG(!is_bound(), ("Trying to bound already bound this to " + parent->location.toString()));
    lf->bracket([this, lf, parent, &name]() {
					this->parent = parent;
                    location = parent->location.sub(name, ".");
					std::cerr << "THIS:" << this << " " << this->location.toString() << "\n";
                    this->bind_lifetime = lf;
                },
                [this, lf]() {
					std::cerr << "THIS:" << this << "\n";
                    this->bind_lifetime = lf;
                    location = location.sub("<<unbound>>", "::");
                    this->parent = nullptr;
                    rdid = RdId::Null();
                }
    );

    get_protocol()->scheduler->assert_thread();

    priorityAdviseSection(
            [this, lf]() {
                init(lf);
            }
    );
}

void RdBindableBase::identify(const IIdentities &identities, RdId const &id) const {
    MY_ASSERT_MSG(rdid.isNull(), "Already has RdId: " + rdid.toString() + ", entity: $this");
    MY_ASSERT_MSG(!id.isNull(), "Assigned RdId mustn't be null, entity: $this");

    this->rdid = id;
    for (const auto &it : bindable_children) {
        identifyPolymorphic(*(it.second), identities, id.mix("." + it.first));
    }
    for (const auto &it : bindable_extensions) {
        identifyPolymorphic(*(it.second), identities, id.mix("." + it.first));
    }
}

const IProtocol *RdBindableBase::get_protocol() const {
    if (is_bound()) {
        auto protocol = parent->get_protocol();
        if (protocol != nullptr) {
            return protocol;
        }
    }
    throw std::invalid_argument("Not bound");
}

SerializationCtx const &RdBindableBase::get_serialization_context() const {
    if (is_bound()) {
        return parent->get_serialization_context();
    } else {
        throw std::invalid_argument("Not bound");
    }
}

void RdBindableBase::init(Lifetime lifetime) const {
    for (const auto &it : bindable_children) {
        if (it.second != nullptr) {
            bindPolymorphic(*(it.second), lifetime, this, it.first);
        }
    }
    for (const auto &it : bindable_extensions) {
        if (it.second != nullptr) {
            bindPolymorphic(*(it.second), lifetime, this, it.first);
        }
    }
}


