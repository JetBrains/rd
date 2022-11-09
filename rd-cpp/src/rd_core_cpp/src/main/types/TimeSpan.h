#ifndef RD_CPP_TIMESPAN_H
#define RD_CPP_TIMESPAN_H

#include <std/hash.h>
#include <string>

#include <rd_core_export.h>

namespace rd
{
/**
 * \brief Wrapper around long to be synchronized with "TimeSpan" in Kt and "TimeSpan" in C#.
 */
class RD_CORE_API TimeSpan
{
public:
	static int64_t TicksPerDay;
	static int64_t TicksPerHour;
	static int64_t TicksPerMinute;
	static int64_t TicksPerSecond;
	static int64_t TicksPerMillisecond;
	static int64_t TicksPerMicrosecond;
	static TimeSpan Zero;
	static TimeSpan Max;

	static TimeSpan CreateFromTicks(int64_t ticks);
	static TimeSpan CreateFromSeconds(int32_t seconds);
	static TimeSpan CreateFromMilliseconds(int32_t milliseconds);

	explicit TimeSpan(int64_t ticks);
		
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

	friend bool RD_CORE_API operator<(const TimeSpan& lhs, const TimeSpan& rhs);
	friend bool RD_CORE_API operator>(const TimeSpan& lhs, const TimeSpan& rhs);
	friend bool RD_CORE_API operator<=(const TimeSpan& lhs, const TimeSpan& rhs);
  friend bool RD_CORE_API operator>=(const TimeSpan& lhs, const TimeSpan& rhs);
  friend bool RD_CORE_API operator==(const TimeSpan& lhs, const TimeSpan& rhs);
  friend bool RD_CORE_API operator!=(const TimeSpan& lhs, const TimeSpan& rhs);
  friend TimeSpan RD_CORE_API operator +(const TimeSpan& lhs, const TimeSpan& rhs);
	friend TimeSpan RD_CORE_API operator -(const TimeSpan& lhs, const TimeSpan& rhs);

	friend std::string RD_CORE_API to_string(TimeSpan const& TimeSpan);

protected:
	int64_t m_ticks;
};
}	 // namespace rd
namespace rd
{
template <>
struct RD_CORE_API hash<rd::TimeSpan>
{
	size_t operator()(const rd::TimeSpan& value) const noexcept;
};
}	 // namespace rd

#endif	  // RD_CPP_TIMESPAN_H
