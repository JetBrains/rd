//
// Created by jetbrains on 24.02.2019.
//

#ifndef RD_CPP_VOID_H
#define RD_CPP_VOID_H

#include <functional>

namespace rd {
	class Void {
		friend bool operator==(const Void &lhs, const Void &rhs);

		friend bool operator!=(const Void &lhs, const Void &rhs);
	};
}

namespace std {
	template<>
	struct hash<rd::Void> {
		size_t operator()(const rd::Void &value) const noexcept {
			return 0;
		}
	};
}

#endif //RD_CPP_VOID_H
