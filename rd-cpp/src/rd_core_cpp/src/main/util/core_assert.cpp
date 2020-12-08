#include "core_assert.h"

#include <cassert>

#ifndef NDEBUG

extern "C" void _rd_assert_impl(const wchar_t* msg, const wchar_t* file, unsigned line)
{
	_wassert(msg, file, line);
}

#endif