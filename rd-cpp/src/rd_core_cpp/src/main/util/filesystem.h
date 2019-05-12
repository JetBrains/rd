//
// Created by jetbrains on 5/9/2019.
//

#ifndef RD_CPP_FILESYSTEM_H
#define RD_CPP_FILESYSTEM_H

#include <string>

#ifdef _WIN32

#include <windows.h>

#endif

namespace rd {
	namespace filesystem {
		std::string get_temp_directory() {
#ifdef _WIN32
			char path[MAX_PATH];
			assert (GetTempPath(MAX_PATH, path));
			return path;
#endif
#ifdef __linux__
			return "/tmp";
#endif
		}
	}
}


#endif //RD_CPP_FILESYSTEM_H
