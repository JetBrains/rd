
#include "RdId.h"

#include "Identities.h"

namespace rd {
	std::string RdId::toString() const {
		return std::to_string(hash);
	}

	RdId RdId::read(Buffer const &buffer) {
		auto number = buffer.read_integral<hash_t>();
		return RdId(number);
	}

	void RdId::write(const Buffer &buffer) const {
		buffer.write_integral(hash);
	}
}

