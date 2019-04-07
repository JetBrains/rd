#include "DefaultAbstractDeclaration.h"

namespace rd {
	const std::string DefaultAbstractDeclaration::notRegisteredErrorMessage = "Maybe you forgot to invoke 'register()' method of corresponding Toplevel. "
																			  "Usually it should be done automagically during 'bind()' invocation but in complex cases you should do it manually.";

	Wrapper<DefaultAbstractDeclaration>
	DefaultAbstractDeclaration::readUnknownInstance(rd::SerializationCtx const &ctx, rd::Buffer const &buffer,
													rd::RdId const &unknownId, int32_t size) {
		throw std::invalid_argument("Can't find reader by id: " + unknownId.toString() + notRegisteredErrorMessage);
	}

	std::string DefaultAbstractDeclaration::type_name() const {
		return "DefaultAbstractDeclaration";
	}

	bool DefaultAbstractDeclaration::equals(ISerializable const &serializable) const {
		return false;
	}

	void DefaultAbstractDeclaration::write(SerializationCtx const &ctx, Buffer const &buffer) const {
		throw std::invalid_argument("DefaultAbstractDeclaration couldn't be written");
	}
}