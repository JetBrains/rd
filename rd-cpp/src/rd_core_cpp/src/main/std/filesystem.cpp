#include "std/filesystem.h"

#ifdef _WIN32

#include <windows.h>

#endif

namespace rd {
	std::string filesystem::get_temp_directory() {
#ifdef _WIN32
		/*char path[MAX_PATH];
		assert(GetTempPath(MAX_PATH, path));
		return path;*/
		return getenv("TEMP");
#endif
#ifdef __linux__
		return "/tmp";
#endif
#ifdef __APPLE__
		return getenv("TMPDIR");
#endif
		//todo check Mac OS
	}
}
