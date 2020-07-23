#include <gtest/gtest.h>

#include "protocol/Buffer.h"
#include "serialization/Polymorphic.h"
#include "serialization/NullableSerializer.h"
#include "serialization/ArraySerializer.h"

#include <random>
#include <numeric>

using namespace rd;

SerializationCtx ctx{nullptr};

TEST(BufferTest, RdVoid)
{
	Buffer buffer;
	using S = Polymorphic<Void>;
	S::write(ctx, buffer, Void{});
	EXPECT_EQ(0, buffer.get_position());
	const auto v = S::read(ctx, buffer);
	EXPECT_EQ(Void{}, v);
	EXPECT_EQ(0, buffer.get_position());
}

TEST(BufferTest, readWritePod)
{
	Buffer buffer;

	buffer.write_integral<int32_t>(0);
	buffer.write_integral<int32_t>(4);
	buffer.write_integral<int64_t>(1ll << 32u);
	buffer.write_integral<int32_t>(-1);
	buffer.write_char('+');
	buffer.write_char('-');
	buffer.write_bool(true);

	EXPECT_EQ(buffer.get_position(), (sizeof(int32_t) + sizeof(int32_t) + sizeof(int64_t) + sizeof(int32_t) + sizeof(uint16_t) +
										 sizeof(uint16_t) + sizeof(bool)));

	buffer.rewind();

	buffer.write_integral<int32_t>(16);
	EXPECT_EQ(4, buffer.read_integral<int32_t>());
	EXPECT_EQ(1ll << 32u, buffer.read_integral<int64_t>());
	EXPECT_EQ(-1, buffer.read_integral<int32_t>());
	EXPECT_EQ('+', buffer.read_char());
	EXPECT_EQ('-', buffer.read_char());
	EXPECT_EQ(true, buffer.read_bool());
}

TEST(BufferTest, getArray)
{
	using W = int32_t;
	const size_t N = 100;
	const size_t MEM = N * sizeof(W);

	Buffer buffer;

	std::vector<W> list(N, -1);
	for (auto t : list)
	{
		buffer.write_integral(t);
	}

	Buffer::ByteArray data(MEM);
	memcpy(data.data(), buffer.data(), MEM);

	const auto array = buffer.getArray();
	const auto realArray = buffer.getRealArray();

	EXPECT_TRUE(array != realArray);
	EXPECT_EQ(realArray.size(), MEM);
	EXPECT_EQ(realArray, data);
}

TEST(BufferTest, string)
{
	Buffer buffer;

	std::wstring s;
	for (int i = 0; i < 255; ++i)
	{
		s += static_cast<wchar_t>(i);
	}

	Polymorphic<std::wstring>::write(ctx, buffer, s);
	buffer.write_integral<int32_t>(static_cast<int32_t>(s.length()));

	EXPECT_EQ(buffer.get_position(), (sizeof(int32_t) +		// length
										 2 * s.size() +		// todo make protocol independent constant
										 sizeof(int32_t)	// length
										 ));

	buffer.rewind();
	const auto res = Polymorphic<std::wstring>::read(ctx, buffer);
	const auto len = buffer.read_integral<int32_t>();
	EXPECT_EQ(s, res);
	EXPECT_EQ(len, s.length());
}

TEST(BufferTest, bigVector)
{
	const int STEP = 100'000;

	Buffer buffer;

	int64_t number = -1;
	std::vector<int64_t> list(STEP);
	std::generate_n(list.begin(), STEP, [&number]() { return --number; });
	std::shuffle(list.begin(), list.end(), std::mt19937(std::random_device()()));

	buffer.write_array(list);

	EXPECT_EQ(buffer.get_position(), (sizeof(int32_t) +	   // length
										 8 * list.size()));

	buffer.rewind();

	const auto res = buffer.read_array<std::vector, int64_t>();

	EXPECT_EQ(res, list);
}

TEST(BufferTest, Enum)
{
	enum class Numbers
	{
		ONE,
		TWO,
		THREE
	};

	Buffer buffer;

	buffer.write_enum<Numbers>(Numbers::ONE);
	buffer.write_enum<Numbers>(Numbers::TWO);
	buffer.write_enum<Numbers>(Numbers::THREE);

	EXPECT_EQ(buffer.get_position(), (3 * 4	   // 3 - quantity,  4 - enum size
										 ));

	buffer.rewind();

	const auto one = buffer.read_enum<Numbers>();
	const auto two = buffer.read_enum<Numbers>();
	const auto three = buffer.read_enum<Numbers>();

	EXPECT_EQ(Numbers::ONE, one);
	EXPECT_EQ(Numbers::TWO, two);
	EXPECT_EQ(Numbers::THREE, three);
}

TEST(BufferTest, EnumSet)
{
	enum class Flags
	{
		ONE = 1 << 0,
		TWO = 1 << 1,
		THREE = 1 << 2
	};

	Buffer buffer;

	buffer.write_enum_set<Flags>(Flags::ONE);
	buffer.write_enum_set<Flags>(Flags::TWO);
	buffer.write_enum_set<Flags>(Flags::THREE);

	EXPECT_EQ(buffer.get_position(), (3 * 4	   // 3 - quantity,  4 - enum size
										 ));

	buffer.rewind();

	const auto one = buffer.read_enum<Flags>();
	const auto two = buffer.read_enum<Flags>();
	const auto three = buffer.read_enum<Flags>();

	EXPECT_EQ(Flags::ONE, one);
	EXPECT_EQ(Flags::TWO, two);
	EXPECT_EQ(Flags::THREE, three);
}

TEST(BufferTest, NullableSerializer)
{
	Buffer buffer;

	using T = std::wstring;
	using S = Polymorphic<T>;
	using NS = NullableSerializer<S>;

	std::vector<Wrapper<T>> list{nullopt, L"1", L"2", nullopt, L"error"};

	buffer.write_integral<int32_t>(+1);
	for (auto const& x : list)
	{
		NS::write(ctx, buffer, x);
	}
	buffer.write_integral<int32_t>(-1);

	const size_t summary_size = std::accumulate(list.begin(), list.end(), 0LL, [](size_t acc, Wrapper<T> const& s) {
		if (s)
		{
			acc += 4 + 2 * s->size();	 // 1 - nullable flag, 4 - length siz, 2 - symbol size
		}
		else
		{
			// nothing because nullable string
		}
		return acc;
	});
	EXPECT_EQ(buffer.get_position(), 4 +							 // first integral
										 (1 + 4 + summary_size) +	 // 1 - nullable flag, 4 - list's size,
										 4);

	buffer.rewind();

	EXPECT_EQ(+1, buffer.read_integral<int32_t>());
	for (auto const& expected : list)
	{
		auto actual = NS::read(ctx, buffer);
		EXPECT_EQ(expected, actual);
	}
	EXPECT_EQ(-1, buffer.read_integral<int32_t>());
}

TEST(BufferTest, ArraySerializer)
{
	Buffer buffer;

	using T = std::wstring;
	using S = Polymorphic<T>;
	using AS = ArraySerializer<S, std::vector>;

	std::vector<Wrapper<T>> list{
		L"start"
		L"1",
		L"2", L"", L"error"};

	buffer.write_integral<int32_t>(+1);
	AS::write(ctx, buffer, list);
	buffer.write_integral<int32_t>(-1);

	const size_t summary_size = std::accumulate(list.begin(), list.end(), 0LL, [](size_t acc, Wrapper<std::wstring> const& s) {
		acc += 4 + 2 * s->size();	 // 4 - length siz, 2 - symbol size
		return acc;
	});
	EXPECT_EQ(buffer.get_position(), (4 +						 // first integral
										 (4 + summary_size) +	 // 4 - list's size,
										 4						 // last integral
										 ));

	buffer.rewind();

	EXPECT_EQ(+1, buffer.read_integral<int32_t>());

	const auto actual = AS::read(ctx, buffer);
	EXPECT_EQ(list, actual);

	EXPECT_EQ(-1, buffer.read_integral<int32_t>());
}

TEST(BufferTest, floating_point)
{
	std::vector<float> float_v{1.0f, -1.0f, -123.456f, 123.456f};
	std::vector<double> double_v{2.0, -2.0, 248.248, -248.248};

	const size_t C = float_v.size();

	Buffer buffer;
	for (size_t i = 0; i < C; ++i)
	{
		buffer.write_floating_point(float_v[i]);
		buffer.write_floating_point(double_v[i]);
	}

	EXPECT_EQ(buffer.get_position(), C * (sizeof(float) + sizeof(double)));
	buffer.rewind();

	for (size_t i = 0; i < C; ++i)
	{
		const auto f = buffer.read_floating_point<float>();
		const auto d = buffer.read_floating_point<double>();
		EXPECT_FLOAT_EQ(f, float_v[i]);
		EXPECT_DOUBLE_EQ(d, double_v[i]);
	}
}

TEST(BufferTest, unsigned_types)
{
	uint8_t val1 = 8;
	uint16_t val2 = 16;
	uint32_t val3 = 32;
	uint64_t val4 = 64;

	Buffer buffer;

	buffer.write_integral<uint8_t>(val1);
	buffer.write_integral<uint16_t>(val2);
	buffer.write_integral<uint32_t>(val3);
	buffer.write_integral<uint64_t>(val4);

	EXPECT_EQ(buffer.get_position(), sizeof(val1) + sizeof(val2) + sizeof(val3) + sizeof(val4));

	buffer.rewind();

	EXPECT_EQ(buffer.read_integral<uint8_t>(), val1);
	EXPECT_EQ(buffer.read_integral<uint16_t>(), val2);
	EXPECT_EQ(buffer.read_integral<uint32_t>(), val3);
	EXPECT_EQ(buffer.read_integral<uint64_t>(), val4);
}

TEST(BufferTest, date_time)
{
	Buffer buffer;

	DateTime time_now{std::time(nullptr)};
	DateTime start_of_epoch{std::time_t{100'000}};

	buffer.write_date_time(time_now);
	buffer.write_date_time(start_of_epoch);

	EXPECT_EQ(2 * sizeof(int64_t), buffer.get_position());

	buffer.rewind();

	auto nt1 = buffer.read_date_time();
	auto nt2 = buffer.read_date_time();

	EXPECT_EQ(time_now, nt1);
	EXPECT_EQ(start_of_epoch, nt2);

	std::cout << std::endl << to_string(time_now) << std::endl << to_string(start_of_epoch);
}
