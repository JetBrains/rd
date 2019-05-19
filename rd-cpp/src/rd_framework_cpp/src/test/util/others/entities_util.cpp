//
// Created by jetbrains on 5/19/2019.
//

#include "entities_util.h"

namespace rd {
	namespace test {
		namespace util {
			DynamicEntity make_dynamic_entity(int32_t x) {
				DynamicEntity res;
				res.get_foo().set(x);
				return res;
			}
		}
	}
}
