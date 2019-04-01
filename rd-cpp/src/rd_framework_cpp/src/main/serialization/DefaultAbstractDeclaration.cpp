//
// Created by jetbrains on 4/1/2019.
//

#include "DefaultAbstractDeclaration.h"

namespace rd {
	const std::string DefaultAbstractDeclaration::notRegisteredErrorMessage = "Maybe you forgot to invoke 'register()' method of corresponding Toplevel. "
																			  "Usually it should be done automagically during 'bind()' invocation but in complex cases you should do it manually.";
}