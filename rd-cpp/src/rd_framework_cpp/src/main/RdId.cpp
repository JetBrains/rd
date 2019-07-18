#include "RdId.h"

#include "Identities.h"

namespace rd {
	RdId RdId::read(Buffer &buffer) {
		const auto number = buffer.read_integral<hash_t>();
		return RdId(number);
	}

	void RdId::write(Buffer &buffer) const {
		buffer.write_integral(hash);
	}

	std::string to_string(RdId const &id) {
		return std::to_string(id.hash);
	}
}

