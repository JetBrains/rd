#ifndef RD_CPP_LINEARIZATION_H
#define RD_CPP_LINEARIZATION_H

#include <cstdint>
#include <mutex>
#include <condition_variable>
#include <chrono>

namespace rd
{
namespace util
{
class Linearization
{
private:
	std::mutex lock;
	std::condition_variable cv;

	bool enabled = true;
	int32_t next_id = 0;

	void set_enable(bool value);

public:
	void point(int32_t id);

	void reset();

	void enable();

	void disable();
};
}	 // namespace util
}	 // namespace rd

#endif	  // RD_CPP_LINEARIZATION_H
