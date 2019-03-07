//
// Created by jetbrains on 23.07.2018.
//


#include "SignalX.h"
#include "RdBindableBase.h"

namespace rd {
	bool RdBindableBase::is_bound() const {
		return parent != nullptr;
	}

	void RdBindableBase::bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const {
		MY_ASSERT_MSG(!is_bound(), ("Trying to bound already bound this to " + parent->location.toString()));
		lf->bracket([this, lf, parent, &name] {
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

		get_protocol()->get_scheduler()->assert_thread();

		priorityAdviseSection(
				[this, lf]() {
					init(lf);
				}
		);
	}

//must be overriden if derived class have bindable members
	void RdBindableBase::identify(const Identities &identities, RdId const &id) const {
		MY_ASSERT_MSG(rdid.isNull(), "Already has RdId: " + rdid.toString() + ", entity: $this");
		MY_ASSERT_MSG(!id.isNull(), "Assigned RdId mustn't be null, entity: $this");

		this->rdid = id;
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
		throw std::invalid_argument("Not bound: " + location.toString());
	}

	SerializationCtx const &RdBindableBase::get_serialization_context() const {
		if (is_bound()) {
			return parent->get_serialization_context();
		} else {
			throw std::invalid_argument("Not bound: " + location.toString());
		}
	}

	void RdBindableBase::init(Lifetime lifetime) const {
		for (const auto &it : bindable_extensions) {
			if (it.second != nullptr) {
				bindPolymorphic(*(it.second), lifetime, this, it.first);
			}
		}
	}
}


