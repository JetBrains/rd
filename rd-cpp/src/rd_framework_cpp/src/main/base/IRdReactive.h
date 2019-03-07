//
// Created by jetbrains on 23.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_IRDREACTIVE_H
#define RD_CPP_FRAMEWORK_IRDREACTIVE_H


#include "IRdBindable.h"
#include "IScheduler.h"

namespace rd {
	class IRdReactive : public virtual IRdBindable {
	public:
		bool async = false;
		//region ctor/dtor

		IRdReactive() = default;

		virtual ~IRdReactive() = default;
		//endregion

		//    IScheduler *wire_scheduler = nullptr;

		virtual IScheduler * get_wire_scheduler() const = 0;

		virtual void on_wire_received(Buffer buffer) const = 0;
	};
}


#endif //RD_CPP_FRAMEWORK_IRDREACTIVE_H
