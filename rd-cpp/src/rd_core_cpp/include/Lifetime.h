#ifndef RD_CPP_CORE_LIFETIMEWRAPPER_H
#define RD_CPP_CORE_LIFETIMEWRAPPER_H


//#include "LifetimeImpl.h"

#include <memory>

namespace rd {
	class Lifetime;
}

namespace std {
	template<>
	struct hash<rd::Lifetime> {
		size_t operator()(const rd::Lifetime &value) const noexcept;
	};
}

namespace rd {
	class LifetimeImpl;

	class Lifetime final {
	private:
		using Allocator = std::allocator<LifetimeImpl>;

		static thread_local Allocator allocator;

		friend class LifetimeDefinition;

		friend struct std::hash<Lifetime>;

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

		bool is_terminated() const;

		Lifetime create_nested() const;

		template<typename F, typename G>
		void bracket(F &&opening, G &&closing);
	};
}

#include "LifetimeImpl.h"

namespace rd {
	template<typename F, typename G>
	void Lifetime::bracket(F &&opening, G &&closing) {
		ptr->bracket(std::forward<F>(opening), std::forward<G>(closing));
	}
}

inline size_t std::hash<rd::Lifetime>::operator()(const rd::Lifetime &value) const noexcept {
	return std::hash<std::shared_ptr<rd::LifetimeImpl> >()(value.ptr);
}

#endif //RD_CPP_CORE_LIFETIMEWRAPPER_H
