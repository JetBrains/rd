//
// Created by jetbrains on 23.07.2018.
//


#include "demangle.h"
#include "RdId.h"
#include "Identities.h"

namespace rd {
	RdId::RdId(hash_t hash) : hash(hash) {}

	namespace {
		RdId NULL_ID(0);
	}

	RdId RdId::Null() {
		return NULL_ID;
	}

	RdId RdId::mix(const std::string &tail) const {
		return RdId(getPlatformIndependentHash(tail, hash));
	}

	RdId RdId::mix(int32_t tail) const {
		return RdId(getPlatformIndependentHash(tail, hash));
	}

	RdId RdId::mix(int64_t tail) const {
		return RdId(getPlatformIndependentHash(tail, hash));
	}

	hash_t RdId::get_hash() const {
		return hash;
	}

	bool RdId::isNull() const {
		return get_hash() == NULL_ID.get_hash();
	}

	std::string RdId::toString() const {
		return std::to_string(hash);
	}

	RdId RdId::notNull() {
		MY_ASSERT_MSG(!isNull(), "id is null");
		return *this;
	}

	RdId RdId::read(Buffer const &buffer) {
		auto number = buffer.read_pod<hash_t>();
		return RdId(number);
	}

	void RdId::write(const Buffer &buffer) const {
		buffer.write_pod(hash);
	}
}

