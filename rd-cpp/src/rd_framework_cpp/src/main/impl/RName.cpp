#include "RName.h"

#include "thirdparty.hpp"

namespace rd {
	RName::RName(RName * parent, string_view localName, string_view separator) : parent(
			parent), local_name(localName), separator(separator) {}

	RName RName::sub(string_view localName, string_view separator) {
		return RName(this, localName, separator);
	}

	namespace {
		RName EMPTY(nullptr, "", "");
	}

	std::string RName::toString() const {
		optional<std::string> res;
		if (parent)
			res = parent->toString();
		if (res && !res->empty()) {
			return *res + separator + local_name;
		} else {
			return local_name;
		}
	}

	RName::RName(string_view local_name) : RName(&EMPTY, local_name, "") {}

	RName RName::Empty() {
		return EMPTY;
	}
}