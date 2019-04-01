//
// Created by jetbrains on 4/1/2019.
//

#ifndef RD_CPP_DEFAULTABSTRACTDECLARATION_H
#define RD_CPP_DEFAULTABSTRACTDECLARATION_H

#include "wrapper.h"
#include "RdId.h"
#include "ISerializable.h"

namespace rd {

	//region predeclared

	class SerializationCtx;

	class Buffer;
	//endregion

	class DefaultAbstractDeclaration : public IPolymorphicSerializable {
		const static std::string notRegisteredErrorMessage;
	public:
		static Wrapper <DefaultAbstractDeclaration>
		readUnknownInstance(rd::SerializationCtx const &ctx, rd::Buffer const &buffer, rd::RdId const &unknownId,
							int32_t size) {
			throw std::invalid_argument("Can't find reader by id: " + unknownId.toString() + notRegisteredErrorMessage);
		}
	};
}


#endif //RD_CPP_DEFAULTABSTRACTDECLARATION_H
