#include <gtest/gtest.h>

#include "util/gen_util.h"

#include "thirdparty.hpp"

using namespace rd;

TEST(hash, contentDeepHashCodeInt)
{
	std::vector<int> a{1, 2, 3};
	std::vector<int> b{1, 2, 3, 4, 5};
	std::vector<std::vector<int>> c{{1, 2, 3}};
	std::vector<std::vector<int>> d{{1, 2, 3}, {4, 5}};
	EXPECT_EQ(contentHashCode(a), contentDeepHashCode(a));
	EXPECT_NE(0, contentDeepHashCode(a));
	EXPECT_NE(contentDeepHashCode(a), contentDeepHashCode(b));
	EXPECT_NE(contentDeepHashCode(a), contentDeepHashCode(c));
	EXPECT_NE(contentDeepHashCode(c), contentDeepHashCode(d));
	EXPECT_NE(contentDeepHashCode(b), contentDeepHashCode(d));
}

TEST(hash, contentDeepHashCodeOptional)
{
	std::vector<optional<int>> a{nullopt, 2, nullopt};
	std::vector<optional<int>> b{nullopt, 2, 3};

	EXPECT_NE(contentDeepHashCode(a), contentHashCode(b));
}