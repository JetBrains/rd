#include "thread_util.h"

#ifdef _WIN32

#include <windows.h>
const DWORD MS_VC_EXCEPTION = 0x406D1388;

#pragma pack(push, 8)
typedef struct tagTHREADNAME_INFO
{
	DWORD dwType;		 // Must be 0x1000.
	LPCSTR szName;		 // Pointer to name (in user addr space).
	DWORD dwThreadID;	 // Thread ID (-1=caller thread).
	DWORD dwFlags;		 // Reserved for future use, must be zero.
} THREADNAME_INFO;
#pragma pack(pop)

void SetThreadName(uint32_t dwThreadID, const char* threadName)
{
	THREADNAME_INFO info;
	info.dwType = 0x1000;
	info.szName = threadName;
	info.dwThreadID = dwThreadID;
	info.dwFlags = 0;

	__try
	{
		RaiseException(MS_VC_EXCEPTION, 0, sizeof(info) / sizeof(ULONG_PTR), (ULONG_PTR*) &info);
	}
	__except (EXCEPTION_EXECUTE_HANDLER)
	{
	}
}
void SetThreadName(const char* threadName)
{
	SetThreadName(GetCurrentThreadId(), threadName);
}

void SetThreadName(std::thread& thread, const char* threadName)
{
	DWORD threadId = ::GetThreadId(static_cast<HANDLE>(thread.native_handle()));
	SetThreadName(threadId, threadName);
}

#else
void SetThreadName(std::thread& thread, const char* threadName)
{
	auto handle = thread.native_handle();
	pthread_setname_np(handle, threadName);
}

#include <sys/prctl.h>
void SetThreadName(const char* threadName)
{
	prctl(PR_SET_NAME, threadName, 0, 0, 0);
}

#endif

namespace rd
{
namespace util
{
void set_thread_name(std::thread& thread, const char* name)
{
	SetThreadName(thread, name);
}

void set_thread_name(const char* name)
{
	SetThreadName(name);
}

}	 // namespace util
}	 // namespace rd
