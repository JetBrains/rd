#include "DateTime.h"

#include <sstream>
#include <iomanip>

namespace rd {
	DateTime::DateTime(time_t value) : value(value) {}

	bool operator<(const DateTime &lhs, const DateTime &rhs) {
		return lhs.value < rhs.value;
	}

	bool operator>(const DateTime &lhs, const DateTime &rhs) {
		return rhs < lhs;
	}

	bool operator<=(const DateTime &lhs, const DateTime &rhs) {
		return !(rhs < lhs);
	}

	bool operator>=(const DateTime &lhs, const DateTime &rhs) {
		return !(lhs < rhs);
	}

	std::string to_string(DateTime const &time) {
		std::stringstream ss;
		ss << std::put_time(std::localtime(&time.value), "%F %T");
		return ss.str();
	}

	bool operator==(const DateTime &lhs, const DateTime &rhs) {
		return lhs.value == rhs.value;
	}

	bool operator!=(const DateTime &lhs, const DateTime &rhs) {
		return !(rhs == lhs);
	}
}

inline size_t std::hash<rd::DateTime>::operator()(const rd::DateTime &value) const noexcept {
	return std::hash<decltype(value.value)>()(value.value);
}