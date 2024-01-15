#ifndef RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H
#define RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H

#include "LifetimeDefinition.h"
#include "Lifetime.h"

#include <rd_core_export.h>

RD_PUSH_STL_EXPORTS_WARNINGS

namespace rd
{
class RD_CORE_API SequentialLifetimes
{
	Lifetime parent_lifetime;
	Lifetime current_lifetime = Lifetime::Terminated();
	void set_current_lifetime(const Lifetime& lifetime);

public:
	// region ctor/dtor
	SequentialLifetimes() = delete;

	SequentialLifetimes(SequentialLifetimes const&) = delete;

	SequentialLifetimes& operator=(SequentialLifetimes const&) = delete;

	SequentialLifetimes(SequentialLifetimes&&) = delete;

	SequentialLifetimes& operator=(SequentialLifetimes&&) = delete;

	explicit SequentialLifetimes(const Lifetime& parent_lifetime);
	// endregion

	Lifetime next();

	void terminate_current();

	bool is_terminated() const;
};
}	 // namespace rd

RD_POP_STL_EXPORTS_WARNINGS

#endif	  // RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H
