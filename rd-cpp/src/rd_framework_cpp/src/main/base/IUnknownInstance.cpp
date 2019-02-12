//
// Created by jetbrains on 22.11.2018.
//

#include "IUnknownInstance.h"

namespace rd {
	IUnknownInstance::IUnknownInstance() {}

	IUnknownInstance::IUnknownInstance(const RdId &unknownId) : unknownId(unknownId) {}

	IUnknownInstance::IUnknownInstance(RdId &&unknownId) : unknownId(std::move(unknownId)) {}
}
