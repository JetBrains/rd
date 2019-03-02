//
// Created by jetbrains on 08.02.2019.
//

#ifndef RD_CPP_INTERNROOT_H
#define RD_CPP_INTERNROOT_H

#include "RdReactiveBase.h"
#include "InternScheduler.h"
#include "IRdReactive.h"

#include <string>

namespace rd {
	//region predeclared

	class IIdentities;
	//endregion

	class InternRoot final : public RdReactiveBase {
	private:
		mutable InternScheduler intern_scheduler;
	public:
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

		void identify(const IIdentities &identities, RdId const &id) const override;

		void on_wire_received(Buffer buffer) const override;
	};
}


#endif //RD_CPP_INTERNROOT_H
