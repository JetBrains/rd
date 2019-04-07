#ifndef RD_CPP_RDTASKIMPL_H
#define RD_CPP_RDTASKIMPL_H

#include "Polymorphic.h"
#include "RdTaskResult.h"

#include "thirdparty.hpp"


namespace rd {
	template<typename T, typename S = Polymorphic<T>>
	class RdTaskImpl {
		template<typename, typename>
		friend
		class RdTask;

		mutable Property<RdTaskResult<T, S>> result;
	};
}


#endif //RD_CPP_RDTASKIMPL_H
