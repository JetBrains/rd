//
// Created by operasfantom on 19.07.18.
//

#include "Lifetime.h"

namespace rd {
	LifetimeImpl *Lifetime::operator->() const {
		return ptr.operator->();
	}

	Lifetime::Lifetime(bool is_eternal) : ptr(std::make_shared<LifetimeImpl>(is_eternal)) {}

	Lifetime Lifetime::create_nested() const {
		Lifetime lw(false);
		ptr->attach_nested(lw.ptr);
		return lw;
	}

	namespace {
		Lifetime ETERNAL(true);
	}

	Lifetime const &Lifetime::Eternal() {
		return ETERNAL;
	}

	bool operator==(Lifetime const &lw1, Lifetime const &lw2) {
		return lw1.ptr == lw2.ptr;
	}

	bool operator!=(Lifetime const &lw1, Lifetime const &lw2) {
		return !(lw1 == lw2);
	}
}
