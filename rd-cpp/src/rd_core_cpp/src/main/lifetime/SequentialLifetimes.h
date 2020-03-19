#ifndef RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H
#define RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H

#include "lifetime/LifetimeDefinition.h"
#include "lifetime/Lifetime.h"

namespace rd {
	class SequentialLifetimes {
	private:
		std::shared_ptr<LifetimeDefinition> current_def = LifetimeDefinition::get_shared_eternal();
		Lifetime parent_lifetime;
	public:
		//region ctor/dtor
		SequentialLifetimes() = delete;

		SequentialLifetimes(SequentialLifetimes const&) = delete;

		SequentialLifetimes &operator=(SequentialLifetimes const &) = delete;

		SequentialLifetimes(SequentialLifetimes &&) = delete;

		SequentialLifetimes &operator=(SequentialLifetimes &&) = delete;

		explicit SequentialLifetimes(Lifetime parent_lifetime);
		//endregion

		Lifetime next();

		void terminate_current();

		bool is_terminated() const;

		void set_current_lifetime(std::shared_ptr<LifetimeDefinition> new_def);
	};
}


#endif //RD_CPP_CORE_SEQUENTIAL_LIFETIMES_H
