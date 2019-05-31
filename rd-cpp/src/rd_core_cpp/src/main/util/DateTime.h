#ifndef RD_CPP_DATETIME_H
#define RD_CPP_DATETIME_H

#include <ctime>
#include <string>

namespace rd {
	class DateTime {
	public:
		std::time_t value;

		explicit DateTime(time_t value);

		friend bool operator<(const DateTime &lhs, const DateTime &rhs);

		friend bool operator>(const DateTime &lhs, const DateTime &rhs);

		friend bool operator<=(const DateTime &lhs, const DateTime &rhs);

		friend bool operator>=(const DateTime &lhs, const DateTime &rhs);

		friend bool operator==(const DateTime &lhs, const DateTime &rhs);

		friend bool operator!=(const DateTime &lhs, const DateTime &rhs);

		friend std::string to_string(DateTime const &time);
	};
}
namespace std {
	template<>
	struct hash<rd::DateTime> {
		size_t operator()(const rd::DateTime &value) const noexcept;
	};
}

#endif //RD_CPP_DATETIME_H
