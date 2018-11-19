//
// Created by jetbrains on 07.10.2018.
//

#include "gtest/gtest.h"

#include <random>

#include "Buffer.h"
#include "Polymorphic.h"
#include "NullableSerializer.h"
#include "ArraySerializer.h"

TEST(BufferTest, readWritePod) {
    Buffer buffer;

    buffer.write_pod<int32_t>(0);
    buffer.write_pod<int32_t>(4);
    buffer.write_pod<int64_t>(1ll << 32);
    buffer.write_pod<int32_t>(-1);
    buffer.write_pod<char>('+');
    buffer.write_pod<char>('-');
    buffer.write_pod<bool>(true);


    buffer.rewind();

    buffer.write_pod<int32_t>(16);
    EXPECT_EQ(4, buffer.read_pod<int32_t>());
    EXPECT_EQ(1ll << 32, buffer.read_pod<int64_t>());
    EXPECT_EQ(-1, buffer.read_pod<int32_t>());
    EXPECT_EQ('+', buffer.read_pod<char>());
    EXPECT_EQ('-', buffer.read_pod<char>());
    EXPECT_EQ(true, buffer.read_pod<bool>());
}


TEST(BufferTest, getArray) {
    using W = int32_t;
    const size_t N = 100;
    const size_t MEM = N * sizeof(W);

    Buffer buffer;

    std::vector<W> list(N, -1);
    for (auto t : list) {
        buffer.write_pod(t);
    }

    Buffer::ByteArray data(MEM);
    memcpy(data.data(), buffer.data(), MEM);

    auto array = buffer.getArray();
    auto realArray = buffer.getRealArray();

    EXPECT_TRUE(array != realArray);
    EXPECT_EQ(realArray.size(), MEM);
    EXPECT_EQ(realArray, data);
}


TEST(BufferTest, string) {
    Buffer buffer;

    std::string s;
    for (int i = 0; i < 255; ++i) {
        s += static_cast<char>(i);
    }

    Polymorphic<std::string>::write(SerializationCtx(), buffer, s);
    buffer.write_pod<int32_t>(s.length());
    buffer.rewind();
    auto res = Polymorphic<std::string>::read(SerializationCtx(), buffer);
    auto len = buffer.read_pod<int32_t>();
    EXPECT_EQ(s, res);
    EXPECT_EQ(len, s.length());
}


TEST(BufferTest, bigVector) {
    const int STEP = 100'000;

    Buffer buffer;

    int64_t number = -1;
    std::vector<int64_t> list(STEP);
    std::generate_n(list.begin(), STEP, [&number]() { return --number; });
    std::shuffle(list.begin(), list.end(), std::mt19937(std::random_device()()));

    buffer.writeArray(list);

    buffer.rewind();

    auto res = buffer.readArray<int64_t>();

    EXPECT_EQ(res, list);
}

TEST(BufferTest, Enum) {
    enum class Numbers {
        ONE,
        TWO,
        THREE
    };

    Buffer buffer;

    buffer.writeEnum<Numbers>(Numbers::ONE);
    buffer.writeEnum<Numbers>(Numbers::TWO);
    buffer.writeEnum<Numbers>(Numbers::THREE);

    buffer.rewind();

    auto one = buffer.readEnum<Numbers>();
    auto two = buffer.readEnum<Numbers>();
    auto three = buffer.readEnum<Numbers>();

    EXPECT_EQ(Numbers::ONE, one);
    EXPECT_EQ(Numbers::TWO, two);
    EXPECT_EQ(Numbers::THREE, three);
}

TEST(BufferTest, NullableSerializer) {
    SerializationCtx ctx;
    Buffer buffer;

    using T = std::string;
    using S = Polymorphic<T>;
    using NS = NullableSerializer<S>;

    std::vector<tl::optional<T>> list{
            tl::nullopt,
            "1",
            "2",
            tl::nullopt,
            "error"
    };

	buffer.write_pod<int32_t>(+1);
    for (auto const &x : list) {
        NS::write(ctx, buffer, x);
    }
	buffer.write_pod<int32_t>(-1);

    buffer.rewind();

	EXPECT_EQ(+1, buffer.read_pod<int32_t>());
    for (auto const &expected : list) {
        auto actual = NS::read(ctx, buffer);
        EXPECT_EQ(expected, actual);
    }
	EXPECT_EQ(-1, buffer.read_pod<int32_t>());
}

TEST(BufferTest, ArraySerializer) {
    SerializationCtx ctx;
    Buffer buffer;

    using T = std::string;
    using S = Polymorphic<T>;
    using AS = ArraySerializer<S>;

    std::vector<T> list{
            "start"
            "1",
            "2",
            "",
            "error"
    };

	buffer.write_pod<int32_t>(+1);
    AS::write(ctx, buffer, list);
	buffer.write_pod<int32_t>(-1);

    buffer.rewind();

    

	EXPECT_EQ(+1, buffer.read_pod<int32_t>());

	auto actual = AS::read(ctx, buffer);
    EXPECT_EQ(list, actual);

	EXPECT_EQ(-1, buffer.read_pod<int32_t>());
}