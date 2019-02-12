//
// Created by jetbrains on 25.07.2018.
//

#include "WireBase.h"

namespace rd {
	void WireBase::advise(Lifetime lifetime, const IRdReactive *entity) const {
		message_broker.advise_on(lifetime, entity);
	}
}
