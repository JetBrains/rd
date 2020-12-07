#ifndef RD_CPP_IPROTOCOL_H
#define RD_CPP_IPROTOCOL_H

#if defined(_MSC_VER)
#pragma warning(push)
#pragma warning(disable : 4251)
#endif

#include "IRdDynamic.h"

#include <serialization/Serializers.h>

#include <memory>

#include <rd_framework_export.h>

namespace rd
{
// region predeclared

class SerializationCtx;
class Identities;
class IScheduler;
class IWire;

// endregion

/**
 * \brief A root node in an object graph which can be synchronized with its remote copy over a network or a similar connection.
 */
class RD_FRAMEWORK_API IProtocol : public IRdDynamic
{
	friend class RdExtBase;

public:
	std::unique_ptr<Serializers> serializers = std::make_unique<Serializers>();

protected:
	std::shared_ptr<Identities> identity;
	IScheduler* scheduler = nullptr;

public:
	std::shared_ptr<IWire> wire;
	// region ctor/dtor

	IProtocol();

	IProtocol(std::shared_ptr<Identities> identity, IScheduler* scheduler, std::shared_ptr<IWire> wire);

	IProtocol(IProtocol&& other) noexcept = default;

	IProtocol& operator=(IProtocol&& other) noexcept = default;

	~IProtocol() override;
	// endregion

	const Identities* get_identity() const
	{
		return identity.get();
	}

	const IProtocol* get_protocol() const override
	{
		return this;
	}

	IScheduler* get_scheduler() const
	{
		return scheduler;
	}

	const IWire* get_wire() const
	{
		return wire.get();
	}

	const Serializers& get_serializers() const
	{
		return *serializers;
	}
};
}	 // namespace rd
#if defined(_MSC_VER)
#pragma warning(pop)
#endif

#endif	  // RD_CPP_IPROTOCOL_H
