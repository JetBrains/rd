#ifndef RD_CPP_IPROTOCOL_H
#define RD_CPP_IPROTOCOL_H


#include "IRdDynamic.h"
#include "serialization/Serializers.h"
#include "protocol/Identities.h"
#include "scheduler/base/IScheduler.h"
#include "base/IWire.h"

#include <memory>

namespace rd {
	//region predeclared

	class SerializationCtx;
	//endregion

	/**
	 * \brief A root node in an object graph which can be synchronized with its remote copy over a network or a similar connection.
	 */
	class IProtocol : public IRdDynamic {
		friend class RdExtBase;
	public:
		std::unique_ptr<Serializers> serializers = std::make_unique<Serializers>();
	protected:
		std::shared_ptr<Identities> identity;
		IScheduler *scheduler = nullptr;
	public:
		std::shared_ptr<IWire> wire;
		//region ctor/dtor

		IProtocol();

		IProtocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire);

		IProtocol(IProtocol &&other) noexcept = default;

		IProtocol &operator=(IProtocol &&other) noexcept = default;

		virtual ~IProtocol();
		//endregion

		const Identities * get_identity() const;

		const IProtocol *get_protocol() const override;

		IScheduler *get_scheduler() const;

		const IWire *get_wire() const;

		const Serializers& get_serializers() const;
	};
}


#endif //RD_CPP_IPROTOCOL_H
