#include "InternRoot.h"

#include "AbstractPolymorphic.h"
#include "InternedAnySerializer.h"

namespace rd {
	InternRoot::InternRoot() {
		async = true;
	}

	IScheduler *InternRoot::get_wire_scheduler() const {
		return &intern_scheduler;
	}

	void InternRoot::on_wire_received(Buffer buffer) const {
		tl::optional<InternedAny> value = InternedAnySerializer::read(get_serialization_context(), buffer);
		if (!value) {
			return;
		}
		int32_t remote_id = buffer.read_integral<int32_t>();
		set_interned_correspondence(remote_id ^ 1, *std::move(value));
		MY_ASSERT_MSG(((remote_id & 1) == 0), "Remote sent ID marked as our own, bug?");
	}

	void InternRoot::bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const {
		MY_ASSERT_MSG(!is_bound(), "Trying to bound already bound $this to ${parent.location}")

		lf->bracket([this, parent, &name] {
			this->parent = parent;
			location = parent->location.sub(name, ".");
		}, [this] {
			location = location.sub("<<unbound>>", "::");
			this->parent = nullptr;
			rdid = RdId::Null();
		});

		myItemsList.clear();
		otherItemsList.clear();
		inverseMap.clear();

		get_protocol()->get_wire()->advise(lf, this);
	}

	void InternRoot::identify(const Identities &identities, RdId const &id) const {
		MY_ASSERT_MSG(rdid.isNull(), "Already has RdId: " + rdid.toString() + ", entity: $this");
		MY_ASSERT_MSG(!id.isNull(), "Assigned RdId mustn't be null, entity: $this");

		rdid = id;
	}
}
