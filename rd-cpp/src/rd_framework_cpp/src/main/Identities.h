//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_IDENTITIES_H
#define RD_CPP_FRAMEWORK_IDENTITIES_H

#include "interfaces.h"
#include "RdId.h"

#include <string>

namespace rd {
	using hash_t = int64_t;

	const hash_t DEFAULT_HASH = 19;
	const hash_t HASH_FACTOR = 31;

//PLEASE DO NOT CHANGE IT!!! IT'S EXACTLY THE SAME ON C# SIDE
	hash_t getPlatformIndependentHash(std::string const &that, hash_t initial = DEFAULT_HASH);

	hash_t getPlatformIndependentHash(int32_t const &that, hash_t initial = DEFAULT_HASH);

	hash_t getPlatformIndependentHash(int64_t const &that, hash_t initial = DEFAULT_HASH);

	class Identities {
	private:
		enum class IdKind {
			Client,
			Server
		};

		mutable int32_t id_acc;
	public:

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
