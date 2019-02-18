//
// Created by jetbrains on 07.10.2018.
//

#include "gtest/gtest.h"

#include "Buffer.h"
#include "Polymorphic.h"
#include "NullableSerializer.h"
#include "ArraySerializer.h"

#include <random>
#include <algorithm>

using namespace rd;

TEST(BufferTest, readWritePod) {
	Buffer buffer;

	buffer.write_integral<int32_t>(0);
	buffer.write_integral<int32_t>(4);
	buffer.write_integral<int64_t>(1ll << 32);
	buffer.write_integral<int32_t>(-1);
	buffer.write_integral<wchar_t>('+');
	buffer.write_integral<wchar_t>('-');
	buffer.writeBool(true);

	EXPECT_EQ(buffer.get_position(), (
			sizeof(int32_t) + sizeof(int32_t) +
			sizeof(int64_t) + +sizeof(int32_t) +
			sizeof(wchar_t) + sizeof(wchar_t) +
			sizeof(bool)));

	buffer.rewind();

	buffer.write_integral<int32_t>(16);
	EXPECT_EQ(4, buffer.read_integral<int32_t>());
	EXPECT_EQ(1ll << 32, buffer.read_integral<int64_t>());
	EXPECT_EQ(-1, buffer.read_integral<int32_t>());
	EXPECT_EQ('+', buffer.read_integral<wchar_t>());
	EXPECT_EQ('-', buffer.read_integral<wchar_t>());
	EXPECT_EQ(true, buffer.readBool());
}


TEST(BufferTest, getArray) {
	using W = int32_t;
	const size_t N = 100;
	const size_t MEM = N * sizeof(W);

	Buffer buffer;

	std::vector<W> list(N, -1);
	for (auto t : list) {
		buffer.write_integral(t);
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

	std::wstring s;
	for (int i = 0; i < 255; ++i) {
		s += static_cast<wchar_t>(i);
	}

	Polymorphic<std::wstring>::write(SerializationCtx(), buffer, s);
	buffer.write_integral<int32_t>(s.length());

	EXPECT_EQ(buffer.get_position(), (
			sizeof(int32_t) + //length
			2 * s.size() + //todo make protocol independent constant
			sizeof(int32_t) //length
	));

	buffer.rewind();
	auto res = Polymorphic<std::wstring>::read(SerializationCtx(), buffer);
	auto len = buffer.read_integral<int32_t>();
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

	EXPECT_EQ(buffer.get_position(), (
			sizeof(int32_t) + //length
			8 * list.size()
	));

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

	EXPECT_EQ(buffer.get_position(), (
			3 * 4 //3 - quantity,  4 - enum size
	));

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

	using T = std::wstring;
	using S = Polymorphic<T>;
	using NS = NullableSerializer<S>;

	std::vector<tl::optional<T>> list{
			tl::nullopt,
			L"1",
			L"2",
			tl::nullopt,
			L"error"
	};

	buffer.write_integral<int32_t>(+1);
	for (auto const &x : list) {
		NS::write(ctx, buffer, x);
	}
	buffer.write_integral<int32_t>(-1);

	int summary_size = std::accumulate(list.begin(), list.end(), 0, [](int acc, tl::optional<T> const &s) {
		if (s) {
			acc += 4 + 2 * s->size(); //1 - nullable flag, 4 - length siz, 2 - symbol size
		} else {
			//nothing because nullable string
		}
		return acc;
	});
	EXPECT_EQ(buffer.get_position(), (
			4 + //first integarl
			(1 + 4 + summary_size) + // 1 - nullable flag, 4 - list's size,
			4 //last integral
	));

	buffer.rewind();

	EXPECT_EQ(+1, buffer.read_integral<int32_t>());
	for (auto const &expected : list) {
		auto actual = NS::read(ctx, buffer);
		EXPECT_EQ(expected, actual);
	}
	EXPECT_EQ(-1, buffer.read_integral<int32_t>());
}

TEST(BufferTest, ArraySerializer) {
	SerializationCtx ctx;
	Buffer buffer;

	using T = std::wstring;
	using S = Polymorphic<T>;
	using AS = ArraySerializer<S>;

	std::vector<T> list{
			L"start"
			L"1",
			L"2",
			L"",
			L"error"
	};

	buffer.write_integral<int32_t>(+1);
	AS::write(ctx, buffer, list);
	buffer.write_integral<int32_t>(-1);

	int summary_size = std::accumulate(list.begin(), list.end(), 0, [](int acc, std::wstring const &s) {
		acc += 4 + 2 * s.size(); //4 - length siz, 2 - symbol size
		return acc;
	});
	EXPECT_EQ(buffer.get_position(), (
			4 + //first integarl
			(4 + summary_size) + //4 - list's size,
			4 //last integral
	));

	buffer.rewind();

	EXPECT_EQ(+1, buffer.read_integral<int32_t>());

	auto actual = AS::read(ctx, buffer);
	EXPECT_EQ(list, actual);

	EXPECT_EQ(-1, buffer.read_integral<int32_t>());
}

TEST(BufferTest, floating_point) {
	std::vector<float> float_v{1.0f, -1.0f, -123.456f, 123.456f};
	std::vector<double> double_v{2.0, -2.0, 248.248, -248.248};

	const int C = float_v.size();

	Buffer buffer;
	for (int i = 0; i < C; ++i) {
		buffer.write_floating_point(float_v[i]);
		buffer.write_floating_point(double_v[i]);
	}

	EXPECT_EQ(buffer.get_position(), C * (sizeof(float) + sizeof(double)));
	buffer.rewind();

	for (int i = 0; i < C; ++i) {
		auto f = buffer.read_floating_point<float>();
		auto d = buffer.read_floating_point<double>();
		EXPECT_FLOAT_EQ(f, float_v[i]);
		EXPECT_FLOAT_EQ(d, double_v[i]);
	}
}