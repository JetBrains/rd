#include "Void.h"

#include <string>

namespace rd {
	bool operator==(const rd::Void &lhs, const rd::Void &rhs) {
		return true;
	}

	bool operator!=(const rd::Void &lhs, const rd::Void &rhs) {
		return !(rhs == lhs);
	}

	std::string to_string(Void const &) {
		return "void";
	}
}
