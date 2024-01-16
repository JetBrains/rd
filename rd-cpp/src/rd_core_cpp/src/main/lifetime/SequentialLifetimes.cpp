#include "SequentialLifetimes.h"

namespace rd
{
SequentialLifetimes::SequentialLifetimes(const Lifetime& parent_lifetime) : parent_lifetime(parent_lifetime)
{
}

Lifetime SequentialLifetimes::next()
{
	Lifetime new_lifetime = parent_lifetime.create_nested();
	set_current_lifetime(new_lifetime);
	return new_lifetime;
}

void SequentialLifetimes::terminate_current()
{
	set_current_lifetime(Lifetime::Terminated());
}

bool SequentialLifetimes::is_terminated() const
{
	return current_lifetime->is_terminated();
}

void SequentialLifetimes::set_current_lifetime(const Lifetime& lifetime)
{
	const Lifetime prev = current_lifetime;
	current_lifetime = lifetime;
	if (!prev->is_terminated())
		prev->terminate();
}
}	 // namespace rd
