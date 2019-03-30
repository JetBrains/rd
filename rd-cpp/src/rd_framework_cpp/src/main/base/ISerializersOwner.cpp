//
// Created by jetbrains on 19.11.2018.
//

#include "ISerializersOwner.h"

namespace rd {
	void ISerializersOwner::registry(Serializers const &serializers) const {
		auto it = used.insert(&serializers);
		if (!it.second) {
			return;
		}

		registerSerializersCore(serializers);
	}
}
