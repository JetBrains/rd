//
// Created by jetbrains on 4/7/2019.
//

#ifndef RD_CPP_GUARDS_H
#define RD_CPP_GUARDS_H

namespace rd {
	namespace util {
		template<typename T>
		class increment_guard {
			T &x;
		public:
			explicit increment_guard(T &x) : x(x) {
				++x;
			}

			~increment_guard() {
				--x;
			}
		};
	}
}

#endif //RD_CPP_GUARDS_H
