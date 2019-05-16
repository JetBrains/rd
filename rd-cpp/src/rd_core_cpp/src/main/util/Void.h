#ifndef RD_CPP_VOID_H
#define RD_CPP_VOID_H

#include <functional>
#include <string>

namespace rd {
	/**
	 * \brief For using in idle events
	 */
	class Void {
		friend bool operator==(const Void &lhs, const Void &rhs);

		friend bool operator!=(const Void &lhs, const Void &rhs);
	};

	std::string to_string(Void const &);
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
