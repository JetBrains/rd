//
// Created by jetbrains on 08.02.2019.
//

#ifndef RD_CPP_INTERNROOT_H
#define RD_CPP_INTERNROOT_H

#include "RdReactiveBase.h"
#include "InternScheduler.h"
#include "wrapper.h"

#include <string>

namespace rd {
	//region predeclared

	class Identities;
	//endregion

	class InternRoot final : public RdReactiveBase {
	private:
		mutable InternScheduler intern_scheduler;

		void set_interned_correspondence(int32_t id, Wrapper<IPolymorphicSerializable> wrapper) const;

	public:
		InternRoot();

		bool is_master;

		template<typename T>
		int32_t intern_value(T &&value) {
			return 0;
			//todo impl
		}

		template<typename T>
		T un_intern_value(int32_t id) {
			return T();
			//todo impl
//			return items;
		}

		IScheduler *get_wire_scheduler() const override;

		void bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const override;

		void identify(const Identities &identities, RdId const &id) const override;

		void on_wire_received(Buffer buffer) const override;
	};
}


#endif //RD_CPP_INTERNROOT_H
