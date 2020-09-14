#ifndef RD_CPP_THREAD_UTIL_H
#define RD_CPP_THREAD_UTIL_H

#include <thread>

namespace rd
{
namespace util
{
void set_thread_name(const char* name);
void set_thread_name(std::thread& thread, const char* name);

}

}	 // namespace rd

#endif	  // RD_CPP_THREAD_UTIL_H
