#include "CrossTestBase.h"

#include "filesystem.h"

const std::string CrossTestBase::tmp_directory = rd::filesystem::get_temp_directory() + "/rd/port.txt";

CrossTestBase::CrossTestBase() = default;

