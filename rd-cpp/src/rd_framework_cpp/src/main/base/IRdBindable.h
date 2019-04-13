#ifndef RD_CPP_FRAMEWORK_IRDBINDABLE_H
#define RD_CPP_FRAMEWORK_IRDBINDABLE_H

#include "IRdDynamic.h"
#include "../../../../rd_core_cpp/include/Lifetime.h"
#include "RdId.h"


namespace rd {
	class Identities;
}

namespace rd {
	class IRdBindable : public IRdDynamic {
	public:
		mutable RdId rdid = RdId::Null();

		mutable IRdDynamic const *parent = nullptr;
		//region ctor/dtor

		IRdBindable() = default;

		IRdBindable(IRdBindable &&other) = default;

		IRdBindable &operator=(IRdBindable &&other) = default;

		virtual ~IRdBindable() = default;
		//endregion

		virtual void bind(Lifetime lf, IRdDynamic const *parent, string_view name) const = 0;

		virtual void identify(Identities const &identities, RdId const &id) const = 0;
	};

	template<typename T>
	typename std::enable_if_t<!std::is_base_of<IRdBindable, typename std::decay_t<T>>::value>
	inline identifyPolymorphic(T &&, Identities const &identities, RdId const &id) {}

//template <>
	inline void identifyPolymorphic(const IRdBindable &that, Identities const &identities, RdId id) {
		that.identify(identities, id);
	}

	template<typename T>
	typename std::enable_if_t<std::is_base_of<IRdBindable, T>::value>
	inline identifyPolymorphic(std::vector<T> const &that, Identities const &identities, RdId const &id) {
		for (size_t i = 0; i < that.size(); ++i) {
			that[i].identify(identities, id.mix(static_cast<int32_t >(i)));
		}
	}

	template<typename T>
	typename std::enable_if_t<!std::is_base_of<IRdBindable, typename std::decay_t<T>>::value>
	inline bindPolymorphic(T &&, Lifetime lf, const IRdDynamic *parent, string_view name) {}

	inline void
	bindPolymorphic(IRdBindable const &that, Lifetime lf, const IRdDynamic *parent, string_view name) {
		that.bind(lf, parent, name);
	}

	template<typename T>
	typename std::enable_if_t<std::is_base_of<IRdBindable, T>::value>
	inline
	bindPolymorphic(std::vector<T> const &that, Lifetime lf, IRdDynamic const *parent, string_view name) {
		for (auto &obj : that) {
			obj.bind(lf, parent, name);
		}
	}
}

#endif //RD_CPP_FRAMEWORK_IRDBINDABLE_H
