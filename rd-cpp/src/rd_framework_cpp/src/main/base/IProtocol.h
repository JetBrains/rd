//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_IPROTOCOL_H
#define RD_CPP_IPROTOCOL_H


#include "Serializers.h"
#include "Identities.h"
#include "IScheduler.h"
#include "IWire.h"
#include "SerializationCtx.h"

#include <memory>

namespace rd {
	//region predeclared

	class IRdDynamic;
	//endregion

	class IProtocol : public IRdDynamic {
	public:
		std::unique_ptr<Serializers> serializers = std::make_unique<Serializers>();
		std::shared_ptr<Identities> identity;
		IScheduler *scheduler = nullptr;
		std::shared_ptr<IWire> wire;
		SerializationCtx context;

		//region ctor/dtor

		IProtocol() = default;

		IProtocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire);

		IProtocol(IProtocol &&other) noexcept = default;

		IProtocol &operator=(IProtocol &&other) noexcept = default;

		virtual ~IProtocol() = default;
		//endregion

		const SerializationCtx &get_serialization_context() const override;
	};
}


#endif //RD_CPP_IPROTOCOL_H
