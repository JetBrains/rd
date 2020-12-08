#ifndef RD_CPP_CORE_ASSERT_H
#define RD_CPP_CORE_ASSERT_H

#include <vcruntime.h>
#include <stdexcept>

#include <rd_core_export.h>

#ifndef RD_ASSERTS_ENABLED
#if NDEBUG
#define RD_ASSERTS_ENABLED 0
#else
#define RD_ASSERTS_ENABLED 1
#endif
#endif

#if RD_ASSERTS_ENABLED
#define _RD_ASSERT_IMPL(msg, file, line) _rd_assert_impl((msg), (file), (line))

extern "C" void RD_CORE_API _rd_assert_impl(const wchar_t* msg, const wchar_t* file, unsigned line);
#else
#define _RD_ASSERT_IMPL(msg, file, line) ((void) 0)
#endif

#define RD_ASSERT_MSG(expr, msg)                                                           \
	do                                                                                     \
	{                                                                                      \
		bool b = (bool) (expr);                                                            \
		if (!b)                                                                            \
		{                                                                                  \
			spdlog::error("{}\n", msg);                                                    \
			_RD_ASSERT_IMPL(_CRT_WIDE(#expr), _CRT_WIDE(__FILE__), (unsigned) (__LINE__)); \
		}                                                                                  \
	} while (false)

#define RD_ASSERT_THROW_MSG(expr, msg)     \
	do                                     \
	{                                      \
		if (!(expr))                       \
		{                                  \
			spdlog::error("{}\n", msg);    \
			throw std::runtime_error(msg); \
		}                                  \
	} while (false)

#endif	  // RD_CPP_CORE_ASSERT_H
