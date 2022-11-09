#include "TimeSpan.h"

#include <sstream>
#include <iomanip>

namespace rd
{
int64_t TimeSpan::TicksPerDay=864000000000LL;
int64_t TimeSpan::TicksPerHour=36000000000LL;
int64_t TimeSpan::TicksPerMinute=600000000;
int64_t TimeSpan::TicksPerSecond=10000000;
int64_t TimeSpan::TicksPerMillisecond=10000;
int64_t TimeSpan::TicksPerMicrosecond=10;

TimeSpan TimeSpan::Zero = TimeSpan{0};
TimeSpan TimeSpan::Max = TimeSpan{18446744073709551615ll};

TimeSpan TimeSpan::CreateFromTicks(int64_t ticks)
{
	return TimeSpan{ticks};
}

TimeSpan TimeSpan::CreateFromSeconds(int32_t seconds)
{
	return TimeSpan{seconds * TicksPerSecond};
}

TimeSpan TimeSpan::CreateFromMilliseconds(int32_t milliseconds)
{
	return TimeSpan{milliseconds * TicksPerSecond};
}

TimeSpan::TimeSpan(int64_t ticks) : m_ticks(ticks)
{
}

int32_t TimeSpan::Days() const
{
	return static_cast<int32_t>(m_ticks / TicksPerDay);
}

int32_t TimeSpan::Hours() const
{
	return static_cast<int32_t>(m_ticks / TicksPerHour %24);
}

int32_t TimeSpan::Minutes() const
{
	return static_cast<int32_t>(m_ticks / TicksPerMinute %60);
}

int32_t TimeSpan::Seconds() const
{
	return static_cast<int32_t>(m_ticks / TicksPerSecond %60);
}

int32_t TimeSpan::Milliseconds() const
{
	return static_cast<int32_t>(m_ticks / TicksPerMillisecond %1000);
}

int32_t TimeSpan::Microseconds() const
{
	return static_cast<int32_t>(m_ticks / TicksPerMicrosecond %1000);
}

int64_t TimeSpan::Ticks() const
{
	return m_ticks;
}

double TimeSpan::TotalDays()const
{
	return static_cast<double>(m_ticks) / static_cast<double_t>(TicksPerDay);
}

double TimeSpan::TotalHours()const
{
	return static_cast<double>(m_ticks) / static_cast<double_t>(TicksPerHour);
}

double TimeSpan::TotalMinutes()const
{
	return static_cast<double>(m_ticks) / static_cast<double_t>(TicksPerMinute);
}

double TimeSpan::TotalSeconds()const
{
	return static_cast<double>(m_ticks) / static_cast<double_t>(TicksPerSecond);
}

double TimeSpan::TotalMilliseconds()const
{
	return static_cast<double>(m_ticks) / static_cast<double_t>(TicksPerMillisecond);
}

double TimeSpan::TotalMicroseconds()const
{
	return static_cast<double>(m_ticks) / static_cast<double_t>(TicksPerMicrosecond);
}

bool operator<(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return lhs.m_ticks < rhs.m_ticks;
}

bool operator>(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return rhs < lhs;
}

bool operator<=(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return !(rhs < lhs);
}

bool operator>=(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return !(lhs < rhs);
}

std::string to_string(TimeSpan const& duration)
{
	std::stringstream ss;
	ss << duration.Days() << "days,"
	   << duration.Hours() << "hours,"
	   << duration.Minutes() << "mins,"
	   << duration.Seconds() << "secs,"
	   << duration.Milliseconds() << "msec,"
	   << duration.Microseconds() << "microsec";
	return ss.str();
}

bool operator==(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return lhs.m_ticks == rhs.m_ticks;
}

bool operator!=(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return !(rhs == lhs);
}

TimeSpan operator+(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return TimeSpan{lhs.m_ticks + rhs.m_ticks};
}

TimeSpan operator-(const TimeSpan& lhs, const TimeSpan& rhs)
{
	return TimeSpan{lhs.m_ticks - rhs.m_ticks};
}

size_t hash<rd::TimeSpan>::operator()(const rd::TimeSpan& value) const noexcept
{
	return rd::hash<decltype(value.Ticks())>()(value.Ticks());
}
}	 // namespace rd
