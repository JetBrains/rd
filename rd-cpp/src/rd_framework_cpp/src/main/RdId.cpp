#include "RdId.h"

#include "Identities.h"

namespace rd {
	std::string RdId::toString() const {
		return std::to_string(hash);
	}

	RdId RdId::read(Buffer &buffer) {
		const auto number = buffer.read_integral<hash_t>();
		return RdId(number);
	}

	void RdId::write(Buffer &buffer) const {
		buffer.write_integral(hash);
	}
}

