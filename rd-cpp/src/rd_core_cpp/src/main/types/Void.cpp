#include "Void.h"

#include <string>

namespace rd
{
bool operator==(const Void& lhs, const Void& rhs)
{
	return true;
}

bool operator!=(const Void& lhs, const Void& rhs)
{
	return !(rhs == lhs);
}

std::string to_string(Void const&)
{
	return "void";
}
}	 // namespace rd
