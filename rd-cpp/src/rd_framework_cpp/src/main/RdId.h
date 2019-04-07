#ifndef RD_CPP_FRAMEWORK_RDID_H
#define RD_CPP_FRAMEWORK_RDID_H

#include "Buffer.h"
#include "hashing.h"

#include "nonstd/string_view.hpp"

#include <cstdint>
#include <string>

#include <memory>

namespace rd {
	class RdId;
}

namespace std {
	template<>
	struct hash<rd::RdId> {
		size_t operator()(const rd::RdId &value) const noexcept;
	};
}

namespace rd {
	class RdId {
	private:
		friend struct std::hash<RdId>;

		using hash_t = util::hash_t;

		constexpr static hash_t NULL_ID = 0;

		hash_t hash{NULL_ID};
	public:
		friend bool operator==(RdId const &left, RdId const &right) {
			return left.hash == right.hash;
		}

		friend bool operator!=(const RdId &lhs, const RdId &rhs) {
			return !(rhs == lhs);
		}

		//region ctor/dtor
		constexpr RdId() = default;

		constexpr RdId(const RdId &other) = default;

		constexpr RdId &operator=(const RdId &other) = default;

		constexpr RdId(RdId &&other) noexcept = default;

		constexpr RdId &operator=(RdId &&other) noexcept = default;

		explicit constexpr RdId(hash_t hash) : hash(hash) {}
		//endregion

//		static const RdId NULL_ID;

		static constexpr RdId Null() {
			return RdId{NULL_ID};
		}

		static constexpr int32_t MAX_STATIC_ID = 1'000'000;

		static RdId read(Buffer const &buffer);

		void write(const Buffer &buffer) const;

		constexpr hash_t get_hash() const {
			return hash;
		}

		constexpr bool isNull() const {
			return get_hash() == NULL_ID;
		}

		std::string toString() const;

		RdId notNull() {
			MY_ASSERT_MSG(!isNull(), "id is null");
			return *this;
		}

		/*template<size_t N>
		constexpr RdId mix(char const (&tail)[N]) const {
			return RdId(util::getPlatformIndependentHash<N>(tail, static_cast<util::constexpr_hash_t>(hash)));
		}*/

		constexpr RdId mix(string_view tail) const {
			return RdId(util::getPlatformIndependentHash(tail, static_cast<util::constexpr_hash_t>(hash)));
		}

		/*constexpr RdId mix(int32_t tail) const {
			return RdId(util::getPlatformIndependentHash(tail, static_cast<util::constexpr_hash_t>(hash)));
		}

		*/
		constexpr RdId mix(int64_t tail) const {
			return RdId(util::getPlatformIndependentHash(tail, static_cast<util::constexpr_hash_t>(hash)));
		}
	};
}


inline size_t std::hash<rd::RdId>::operator()(const rd::RdId &value) const noexcept {
	return std::hash<rd::RdId::hash_t>()(value.hash);
}

#endif //RD_CPP_FRAMEWORK_RDID_H
