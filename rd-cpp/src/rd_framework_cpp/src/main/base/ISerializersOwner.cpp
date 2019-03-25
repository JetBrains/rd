//
// Created by jetbrains on 19.11.2018.
//

#include "ISerializersOwner.h"

//#include "Protocol.h"

namespace rd {
	void ISerializersOwner::registry(Serializers const &serializers) const {
//    val key = this::class
//    if (!serializers.toplevels.add(key)) return
//todo

//		Protocol::initializationLogger.trace("REGISTER serializers for ${key.simpleName}");
		registerSerializersCore(serializers);
	}
}
