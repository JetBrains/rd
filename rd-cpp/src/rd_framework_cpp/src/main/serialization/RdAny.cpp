//
// Created by jetbrains on 3/7/2019.
//

#include "RdAny.h"

namespace rd {
	namespace any {
		template<>
		InternedAny make_interned_any<std::wstring>(const Wrapper <std::wstring> &wrapper) {
			return {wrapper};
		}
	}
}