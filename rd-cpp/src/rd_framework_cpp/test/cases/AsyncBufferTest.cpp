#include <gtest/gtest.h>

#include "ByteBufferAsyncProcessor.h"

using namespace rd;

TEST(AsyncBuffer, order) {
	const int REPEAT = 100;

	int cnt = 0;
	ByteBufferAsyncProcessor buffer("Test", [&cnt](Buffer::ByteArray array) mutable {
		Buffer buffer(std::move(array));
		EXPECT_EQ(cnt, buffer.read_integral<int32_t>());
		EXPECT_TRUE(buffer.get_position() == buffer.size());
		++cnt;
	});

	buffer.start();

	for (int32_t i = 0; i < REPEAT; ++i) {
		if (i == REPEAT / 2) {
			buffer.stop();
		}

		Buffer tmp;
		tmp.write_integral(i);
		buffer.put(std::move(tmp).getRealArray());
	}

	std::this_thread::sleep_for(std::chrono::milliseconds(500));

	EXPECT_EQ(cnt, REPEAT / 2);
}