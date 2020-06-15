#ifndef RD_CPP_FIX_HO_SPDLOG_H
#define RD_CPP_FIX_HO_SPDLOG_H

#include "spdlog/spdlog-inl.h"

namespace spdlog
{
logger* default_logger_raw();
namespace sinks
{
#ifdef _WIN32
using stdout_color_sink_mt = wincolor_stdout_sink_mt;
using stdout_color_sink_st = wincolor_stdout_sink_st;
using stderr_color_sink_mt = wincolor_stderr_sink_mt;
using stderr_color_sink_st = wincolor_stderr_sink_st;
#else
using stdout_color_sink_mt = ansicolor_stdout_sink_mt;
using stdout_color_sink_st = ansicolor_stdout_sink_st;
using stderr_color_sink_mt = ansicolor_stderr_sink_mt;
using stderr_color_sink_st = ansicolor_stderr_sink_st;
#endif
}
}

#endif	  // RD_CPP_FIX_HO_SPDLOG_H
