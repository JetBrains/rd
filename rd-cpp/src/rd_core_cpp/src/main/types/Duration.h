#ifndef RD_CPP_DURATION_H
#define RD_CPP_DURATION_H

#include <std/hash.h>
#include <string>

#include <rd_core_export.h>

namespace rd
{
/**
 * \brief Wrapper around long to be synchronized with "Duration" in Kt and "TimeSpan" in C#.
 */
class RD_CORE_API Duration
{
public:
	static int64_t TicksPerDay;
	static int64_t TicksPerHour;
	static int64_t TicksPerMinute;
	static int64_t TicksPerSecond;
	static int64_t TicksPerMillisecond;
	static int64_t TicksPerMicrosecond;
	static Duration Zero;
	static Duration Max;

	static Duration CreateFromTicks(int64_t ticks);
	static Duration CreateFromSeconds(int32_t seconds);
	static Duration CreateFromMilliseconds(int32_t milliseconds);

	explicit Duration(int64_t ticks);
		
	int32_t Days() const;
	int32_t Hours() const;
	int32_t Minutes() const;
	int32_t Seconds() const;
	int32_t Milliseconds() const;
	int32_t Microseconds() const;
	int64_t Ticks() const;

	double_t TotalDays() const;
	double_t TotalHours() const;
	double_t TotalMinutes() const;
	double_t TotalSeconds() const;
	double_t TotalMilliseconds() const;
	double_t TotalMicroseconds() const;

	friend bool RD_CORE_API operator<(const Duration& lhs, const Duration& rhs);
	friend bool RD_CORE_API operator>(const Duration& lhs, const Duration& rhs);
	friend bool RD_CORE_API operator<=(const Duration& lhs, const Duration& rhs);
  friend bool RD_CORE_API operator>=(const Duration& lhs, const Duration& rhs);
  friend bool RD_CORE_API operator==(const Duration& lhs, const Duration& rhs);
  friend bool RD_CORE_API operator!=(const Duration& lhs, const Duration& rhs);
  friend Duration RD_CORE_API operator +(const Duration& lhs, const Duration& rhs);
	friend Duration RD_CORE_API operator -(const Duration& lhs, const Duration& rhs);

	friend std::string RD_CORE_API to_string(Duration const& duration);

protected:
	int64_t m_ticks;
};
}	 // namespace rd
namespace rd
{
template <>
struct RD_CORE_API hash<rd::Duration>
{
	size_t operator()(const rd::Duration& value) const noexcept;
};
}	 // namespace rd

#endif	  // RD_CPP_DURATION_H
