#include "Duration.h"

#include <sstream>
#include <iomanip>

namespace rd
{
int64_t Duration::TicksPerDay=864000000000LL;
int64_t Duration::TicksPerHour=36000000000LL;
int64_t Duration::TicksPerMinute=600000000;
int64_t Duration::TicksPerSecond=10000000;
int64_t Duration::TicksPerMillisecond=10000;
int64_t Duration::TicksPerMicrosecond=10;

Duration Duration::Zero = Duration{0};
Duration Duration::Max = Duration{18446744073709551615ll};

Duration Duration::CreateFromTicks(int64_t ticks)
{
	return Duration{ticks};
}

Duration Duration::CreateFromSeconds(int32_t seconds)
{
	return Duration{seconds * TicksPerSecond};
}

Duration Duration::CreateFromMilliseconds(int32_t milliseconds)
{
	return Duration{milliseconds * TicksPerSecond};
}

Duration::Duration(int64_t ticks) : m_ticks(ticks)
{
}

int32_t Duration::Days() const
{
	return static_cast<int32_t>(m_ticks / TicksPerDay);
}

int32_t Duration::Hours() const
{
	return static_cast<int32_t>(m_ticks / TicksPerHour %24);
}

int32_t Duration::Minutes() const
{
	return static_cast<int32_t>(m_ticks / TicksPerMinute %60);
}

int32_t Duration::Seconds() const
{
	return static_cast<int32_t>(m_ticks / TicksPerSecond %60);
}

int32_t Duration::Milliseconds() const
{
	return static_cast<int32_t>(m_ticks / TicksPerMillisecond %1000);
}

int32_t Duration::Microseconds() const
{
	return static_cast<int32_t>(m_ticks / TicksPerMicrosecond %1000);
}

int64_t Duration::Ticks() const
{
	return m_ticks;
}

double_t Duration::TotalDays()const
{
	return static_cast<double_t>(m_ticks) / static_cast<double_t>(TicksPerDay);
}

double_t Duration::TotalHours()const
{
	return static_cast<double_t>(m_ticks) / static_cast<double_t>(TicksPerHour);
}

double_t Duration::TotalMinutes()const
{
	return static_cast<double_t>(m_ticks) / static_cast<double_t>(TicksPerMinute);
}

double_t Duration::TotalSeconds()const
{
	return static_cast<double_t>(m_ticks) / static_cast<double_t>(TicksPerSecond);
}

double_t Duration::TotalMilliseconds()const
{
	return static_cast<double_t>(m_ticks) / static_cast<double_t>(TicksPerMillisecond);
}

double_t Duration::TotalMicroseconds()const
{
	return static_cast<double_t>(m_ticks) / static_cast<double_t>(TicksPerMicrosecond);
}

bool operator<(const Duration& lhs, const Duration& rhs)
{
	return lhs.m_ticks < rhs.m_ticks;
}

bool operator>(const Duration& lhs, const Duration& rhs)
{
	return rhs < lhs;
}

bool operator<=(const Duration& lhs, const Duration& rhs)
{
	return !(rhs < lhs);
}

bool operator>=(const Duration& lhs, const Duration& rhs)
{
	return !(lhs < rhs);
}

std::string to_string(Duration const& duration)
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

bool operator==(const Duration& lhs, const Duration& rhs)
{
	return lhs.m_ticks == rhs.m_ticks;
}

bool operator!=(const Duration& lhs, const Duration& rhs)
{
	return !(rhs == lhs);
}

Duration operator+(const Duration& lhs, const Duration& rhs)
{
	return Duration{lhs.m_ticks + rhs.m_ticks};
}

Duration operator-(const Duration& lhs, const Duration& rhs)
{
	return Duration{lhs.m_ticks - rhs.m_ticks};
}

size_t hash<rd::Duration>::operator()(const rd::Duration& value) const noexcept
{
	return rd::hash<decltype(value.Ticks())>()(value.Ticks());
}
}	 // namespace rd
