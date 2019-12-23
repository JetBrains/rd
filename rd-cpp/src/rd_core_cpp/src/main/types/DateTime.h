#ifndef RD_CPP_DATETIME_H
#define RD_CPP_DATETIME_H

#include "std/hash.h"

#include <ctime>
#include <string>

namespace rd {
	/**
	 * \brief Wrapper around time_t to be synchronized with "Date" in Kt and "DateTime" in C#.
	 */
	class DateTime {
	public:
		std::time_t seconds;

		explicit DateTime(time_t seconds);

		friend bool operator<(const DateTime &lhs, const DateTime &rhs);

		friend bool operator>(const DateTime &lhs, const DateTime &rhs);

		friend bool operator<=(const DateTime &lhs, const DateTime &rhs);

		friend bool operator>=(const DateTime &lhs, const DateTime &rhs);

		friend bool operator==(const DateTime &lhs, const DateTime &rhs);

		friend bool operator!=(const DateTime &lhs, const DateTime &rhs);

		//"1970-01-01 03:01:38" for example
		friend std::string to_string(DateTime const &time);
	};
}
namespace rd {
	template<>
	struct hash<rd::DateTime> {
		size_t operator()(const rd::DateTime &value) const noexcept;
	};
}

#endif //RD_CPP_DATETIME_H
