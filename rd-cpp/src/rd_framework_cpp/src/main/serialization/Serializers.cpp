//
// Created by jetbrains on 3/3/2019.
//

#include "Serializers.h"

namespace rd {
	RdId Serializers::real_rd_id(const IUnknownInstance &value) {
		return value.unknownId;
	}

	RdId Serializers::real_rd_id(const IPolymorphicSerializable &value) {
		return RdId(getPlatformIndependentHash(value.type_name()));
	}
}
