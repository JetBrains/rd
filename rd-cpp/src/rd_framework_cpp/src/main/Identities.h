//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_IDENTITIES_H
#define RD_CPP_FRAMEWORK_IDENTITIES_H

#include "interfaces.h"
#include "RdId.h"

#include "nonstd/string_view.hpp"

#include <cstring>
#include <string>
#include <atomic>
#include <type_traits>
#include <climits>

namespace rd {
	using constexpr_hash_t = uint64_t;//RdId::hash_t;

	constexpr constexpr_hash_t DEFAULT_HASH = 19;
	constexpr constexpr_hash_t HASH_FACTOR = 31;

//PLEASE DO NOT CHANGE IT!!! IT'S EXACTLY THE SAME ON C# SIDE
	constexpr RdId::hash_t hashImpl(constexpr_hash_t initial, char const *begin, char const *end) {
		return (begin == end) ? initial : hashImpl(initial * HASH_FACTOR + *begin, begin + 1, end);
	}

	template<size_t N>
	constexpr RdId::hash_t getPlatformIndependentHash(char const that[N], constexpr_hash_t initial = DEFAULT_HASH) {
		return static_cast<RdId::hash_t>(hashImpl(initial, &that[0], &that[N]));
	}

	constexpr RdId::hash_t getPlatformIndependentHash(std::string const &that, constexpr_hash_t initial = DEFAULT_HASH) {
		return static_cast<RdId::hash_t>(hashImpl(initial, &(*that.begin()), &(*that.end())));
	}

	constexpr RdId::hash_t getPlatformIndependentHash(int32_t const &that, constexpr_hash_t initial = DEFAULT_HASH) {
		return static_cast<RdId::hash_t>(initial * HASH_FACTOR + (that + 1));
	}

	constexpr RdId::hash_t getPlatformIndependentHash(int64_t const &that, constexpr_hash_t initial = DEFAULT_HASH) {
		return static_cast<RdId::hash_t>(initial * HASH_FACTOR + (that + 1));
	}

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
