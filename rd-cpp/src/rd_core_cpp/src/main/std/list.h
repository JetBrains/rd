#ifndef RD_CPP_LIST_H
#define RD_CPP_LIST_H

#include <vector>
#include <cstdint>

namespace rd {
	template<typename T>
	int32_t size(T const &value);

	template<typename T, typename A>
	int32_t size(std::vector<T, A> const &value) {
		return static_cast<int32_t>(value.size());
	}

	template<typename T>
	void resize(T &value, int32_t size);

	template<typename T, typename A>
	void resize(std::vector<T, A> &value, int32_t size) {
		value.resize(size);
	}
}


#endif //RD_CPP_LIST_H
