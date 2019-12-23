#ifndef RD_CPP_IUNKNOWNINSTANCE_H
#define RD_CPP_IUNKNOWNINSTANCE_H

#include "protocol/RdId.h"

namespace rd {
	class IUnknownInstance {
	public:
		RdId unknownId{0};

		IUnknownInstance();

		explicit IUnknownInstance(const RdId &unknownId);

		explicit IUnknownInstance(RdId &&unknownId);
	};
}


#endif //RD_CPP_IUNKNOWNINSTANCE_H
