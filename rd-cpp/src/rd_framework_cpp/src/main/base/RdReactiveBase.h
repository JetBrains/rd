#ifndef RD_CPP_RDREACTIVEBASE_H
#define RD_CPP_RDREACTIVEBASE_H

#include "base/RdBindableBase.h"
#include "base/IRdReactive.h"
#include "guards.h"

#include "spdlog/spdlog.h"

#include <rd_framework_export.h>

namespace rd
{
// region predeclared

class IWire;

class IProtocol;

class Serializers;
// endregion

class RD_FRAMEWORK_API RdReactiveBase : public RdBindableBase, public IRdReactive
{
public:
	// region ctor/dtor

	RdReactiveBase() = default;

	RdReactiveBase(RdReactiveBase&& other);

	RdReactiveBase& operator=(RdReactiveBase&& other);

	virtual ~RdReactiveBase() = default;
	// endregion

	const IWire* get_wire() const;

	mutable bool is_local_change = false;

	// delegated

	const RName& get_location() const override { return RdBindableBase::get_location(); }

	const IProtocol* get_protocol() const override { return RdBindableBase::get_protocol(); }

	SerializationCtx& get_serialization_context() const override { return RdBindableBase::get_serialization_context(); }

	void set_id(RdId id) const override { RdBindableBase::set_id(id); }

	RdId get_id() const override { return RdBindableBase::get_id(); }

	void bind(Lifetime lf, IRdDynamic const* parent, string_view name) const override { RdBindableBase::bind(lf, parent, name); }

	void identify(Identities const& identities, RdId const& id) const override { RdBindableBase::identify(identities, id); }

	const Serializers& get_serializers() const;

	IScheduler* get_default_scheduler() const;

	IScheduler* get_wire_scheduler() const override;

	void assert_threading() const;

	void assert_bound() const;

	template <typename F>
	auto local_change(F&& action) const -> typename util::result_of_t<F()>
	{
		if (is_bound() && !async)
		{
			assert_threading();
		}

		RD_ASSERT_MSG(!is_local_change, "!isLocalChange for RdReactiveBase with id:" + to_string(rdid));

		util::bool_guard bool_guard(is_local_change);
		return action();
	}
};
}	 // namespace rd

#endif	  // RD_CPP_RDREACTIVEBASE_H
