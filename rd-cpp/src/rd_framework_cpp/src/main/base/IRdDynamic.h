#ifndef RD_CPP_IRDDYNAMIC_H
#define RD_CPP_IRDDYNAMIC_H

#include "RName.h"

namespace rd {
	//region predeclared

	class IProtocol;

	class SerializationCtx;
	//endregion

	class IRdDynamic {
	public:
		mutable RName location;

		//region ctor/dtor

		IRdDynamic() = default;

		IRdDynamic(IRdDynamic &&other) = default;

		IRdDynamic &operator=(IRdDynamic &&other) = default;

		virtual ~IRdDynamic() = default;
		//endregion

		virtual const IProtocol *get_protocol() const = 0;

		virtual SerializationCtx const &get_serialization_context() const = 0;
	};
}


#endif //RD_CPP_IRDDYNAMIC_H
