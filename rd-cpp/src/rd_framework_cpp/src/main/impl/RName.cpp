//
// Created by jetbrains on 23.07.2018.
//

#include "RName.h"

#include "optional.hpp"

RName::RName(RName *const parent, const std::string &localName, const std::string &separator) : parent(
		parent), local_name(localName), separator(separator) {}

RName RName::sub(const std::string &localName, const std::string &separator) {
	return RName(this, localName, separator);
}

namespace {
	RName EMPTY(nullptr, "", "");
}

std::string RName::toString() const {
	tl::optional<std::string> res;
	if (parent)
		res = parent->toString();
	if (res && !res->empty()) {
		return *res + separator + local_name;
	} else {
		return local_name;
	}
}

RName::RName(const std::string &local_name) : RName(&EMPTY, local_name, "") {}

RName RName::Empty() {
	return EMPTY;
}
