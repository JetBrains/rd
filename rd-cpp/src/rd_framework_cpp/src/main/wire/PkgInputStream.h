#ifndef RD_CPP_PKGINPUTSTREAM_H
#define RD_CPP_PKGINPUTSTREAM_H

#include "protocol/Buffer.h"

namespace rd {
	class PkgInputStream {
	private:
		Buffer buffer;

		std::function<int32_t()> request_data;

		size_t memory = 0;
	public:
		template<typename F>
		explicit PkgInputStream(F &&f) : request_data(std::forward<F>(f)) {}
		
		void rewind();

		void require_available(int size);

		size_t get_position() const;

		Buffer::word_t *data();

		Buffer &get_buffer();

		int32_t try_read(Buffer::word_t *res, size_t size);

		bool read(Buffer::word_t *res, size_t size);

		template<typename T>
		T read_integral() {
			T x{};
			if (!read(reinterpret_cast<Buffer::word_t*>(&x), sizeof(T))) {
				return -1;
			}
			return x;
		}
	};
}


#endif //RD_CPP_PKGINPUTSTREAM_H
