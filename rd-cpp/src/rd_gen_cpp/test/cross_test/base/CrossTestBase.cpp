#include "CrossTestBase.h"

#include "std/filesystem.h"

namespace rd {
	namespace cross {
		const std::string CrossTestBase::port_file = rd::filesystem::get_temp_directory() + "/rd/port.txt";
		const std::string CrossTestBase::port_file_closed = rd::filesystem::get_temp_directory() + "/rd/port.txt.stamp";


		CrossTestBase::CrossTestBase() = default;
	}
}
