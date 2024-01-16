#ifndef LIFETIME_UTIL_H
#define LIFETIME_UTIL_H

#include <memory>
#include "../lifetime/LifetimeDefinition.h"

namespace rd
{
namespace util
{
/// \brief Attaches lifetime to shared_ptr. Lifetime terminates when shared_ptr destructed.
/// \param original original pointer which will be used to make a new pointer with lifetime.
/// \param lifetime_definition Lifetime definition associated with returned shared_ptr.
/// \return New shared_ptr which owns lifetime_definition and terminates that lifetime when destroyed.
template <typename T>
static std::shared_ptr<T> attach_lifetime(std::shared_ptr<T> original, LifetimeDefinition lifetime_definition)
{
	struct Deleter
	{
		std::shared_ptr<T> ptr;
		LifetimeDefinition lifetime_definition;

		explicit Deleter(LifetimeDefinition&& lifetime_definition, std::shared_ptr<T> ptr) : ptr(std::move(ptr)), lifetime_definition(std::move(lifetime_definition)) { }

		void operator()(T*) const
		{
		}
	};

	auto raw_ptr = original.get();
	auto deleter = Deleter(std::move(lifetime_definition), std::move(original));
	return std::shared_ptr<T>(raw_ptr, std::move(deleter));
}
}
}

#endif //LIFETIME_UTIL_H
