#ifndef RD_CPP_DEFAULTABSTRACTDECLARATION_H
#define RD_CPP_DEFAULTABSTRACTDECLARATION_H

#include "wrapper.h"
#include "RdId.h"
#include "ISerializable.h"
#include "IUnknownInstance.h"

namespace rd {

	//region predeclared

	class SerializationCtx;

	class Buffer;
	//endregion

	class DefaultAbstractDeclaration : public IPolymorphicSerializable, public IUnknownInstance {
		const static std::string notRegisteredErrorMessage;
	public:
		static Wrapper<DefaultAbstractDeclaration>
		readUnknownInstance(rd::SerializationCtx const &ctx, rd::Buffer const &buffer, rd::RdId const &unknownId,
							int32_t size);

		std::string type_name() const override;

		bool equals(ISerializable const &serializable) const override;

		void write(SerializationCtx const &ctx, Buffer const &buffer) const override;
	};
}


#endif //RD_CPP_DEFAULTABSTRACTDECLARATION_H
