#ifndef RD_CPP_CORE_LIFETIMEWRAPPER_H
#define RD_CPP_CORE_LIFETIMEWRAPPER_H


#include "LifetimeImpl.h"

#include "std/hash.h"

#include <memory>

namespace rd {
	class Lifetime;

	template<>
	struct hash<Lifetime> {
		size_t operator()(const Lifetime &value) const noexcept;
	};

	class Lifetime final {
	private:
		using Allocator = std::allocator<LifetimeImpl>;

		static /*thread_local */Allocator allocator;

		friend class LifetimeDefinition;

		friend struct hash<Lifetime>;

		std::shared_ptr<LifetimeImpl> ptr;
	public:
		static Lifetime const &Eternal();

		//region ctor/dtor

		Lifetime() = delete;

		Lifetime(Lifetime const &other) = default;

		Lifetime &operator=(Lifetime const &other) = default;

		Lifetime(Lifetime &&other) noexcept = default;

		Lifetime &operator=(Lifetime &&other) noexcept = default;

		~Lifetime() = default;
		//endregion

		friend bool operator==(Lifetime const &lw1, Lifetime const &lw2);

		explicit Lifetime(bool is_eternal = false);

		LifetimeImpl *operator->() const;

		Lifetime create_nested() const;
	};

	inline size_t hash<Lifetime>::operator()(const Lifetime &value) const noexcept {
		return hash<std::shared_ptr<LifetimeImpl> >()(value.ptr);
	}
}


#endif //RD_CPP_CORE_LIFETIMEWRAPPER_H
