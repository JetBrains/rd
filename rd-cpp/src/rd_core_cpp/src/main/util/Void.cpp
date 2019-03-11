//
// Created by jetbrains on 24.02.2019.
//

#include "Void.h"

namespace rd {
	bool operator==(const rd::Void &lhs, const rd::Void &rhs) {
		return true;
	}

	bool operator!=(const rd::Void &lhs, const rd::Void &rhs) {
		return !(rhs == lhs);
	}
}
