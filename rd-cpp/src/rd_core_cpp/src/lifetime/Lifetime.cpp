#include "Lifetime.h"

#include "LifetimeImpl.h"

#include <memory>

namespace rd {
	thread_local Lifetime::Allocator Lifetime::allocator;

	LifetimeImpl *Lifetime::operator->() const {
		return ptr.operator->();
	}

	Lifetime::Lifetime(bool is_eternal) : ptr(std::allocate_shared<LifetimeImpl, Allocator>(allocator, is_eternal)) {}

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

	bool Lifetime::is_terminated() const {
		return ptr->is_terminated();
	}

	bool operator!=(Lifetime const &lw1, Lifetime const &lw2) {
		return !(lw1 == lw2);
	}
}
