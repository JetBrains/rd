//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_PROTOCOL_H
#define RD_CPP_PROTOCOL_H


#include "IProtocol.h"
#include "Identities.h"
#include "SerializationCtx.h"

#include <memory>

namespace rd {
	class Protocol : /*IRdDynamic, */public IProtocol {
		constexpr static auto InternRootName = "ProtocolInternRoot";

		Lifetime lifetime;

		mutable std::unique_ptr<SerializationCtx> context;

		std::unique_ptr<InternRoot> internRoot;

		//region ctor/dtor
	private:
		void initialize();
	public:
		Protocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire, Lifetime lifetime);

		Protocol(Identities::IdKind, IScheduler *scheduler, std::shared_ptr<IWire> wire, Lifetime lifetime);

		Protocol(Protocol const &) = delete;

		Protocol(Protocol &&) noexcept = default;

		Protocol &operator=(Protocol &&) noexcept = default;
		//endregion

		const SerializationCtx &get_serialization_context() const override;

		static const Logger initializationLogger;
	};
}


#endif //RD_CPP_PROTOCOL_H
