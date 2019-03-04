//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_IDENTITIES_H
#define RD_CPP_FRAMEWORK_IDENTITIES_H

#include "RdId.h"

#include <atomic>

namespace rd {
	class Identities {
	private:
		mutable std::atomic_int32_t id_acc;
	public:
		enum class IdKind {
			Client,
			Server
		};

		constexpr static IdKind SERVER = IdKind::Server;
		constexpr static IdKind CLIENT = IdKind::Client;

		constexpr static const int32_t BASE_CLIENT_ID = RdId::MAX_STATIC_ID;

		constexpr static const int32_t BASE_SERVER_ID = RdId::MAX_STATIC_ID + 1;

		//region ctor/dtor

		explicit Identities(IdKind dynamicKind);

		virtual ~Identities() = default;
		//endregion

		RdId next(const RdId &parent) const;
	};
}


#endif //RD_CPP_FRAMEWORK_IDENTITIES_H
