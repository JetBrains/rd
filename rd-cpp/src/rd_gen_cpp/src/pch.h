#ifndef RD_GEN_PCH_H
#define RD_GEN_PCH_H

#include "protocol/Buffer.h"
#include "protocol/Identities.h"
#include "protocol/Protocol.h"
#include "protocol/RdId.h"
#include "impl/RdSignal.h"
#include "impl/RdProperty.h"
#include "impl/RdList.h"
#include "impl/RdSet.h"
#include "impl/RdMap.h"
#include "serialization/ISerializable.h"
#include "base/ISerializersOwner.h"
#include "base/IUnknownInstance.h"
#include "serialization/Polymorphic.h"
#include "serialization/NullableSerializer.h"
#include "serialization/ArraySerializer.h"
#include "serialization/InternedSerializer.h"
#include "serialization/SerializationCtx.h"
#include "serialization/Serializers.h"
#include "ext/RdExtBase.h"
#include "task/RdCall.h"
#include "task/RdEndpoint.h"
#include "RdTask.h"
#include "util/gen_util.h"

#endif //RD_GEN_PCH_H