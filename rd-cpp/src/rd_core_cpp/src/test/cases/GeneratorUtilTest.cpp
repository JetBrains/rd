#include <gtest/gtest.h>

#include "gen_util.h"

#include "optional.hpp"

using namespace rd;

TEST(hash, contentDeepHashCodeInt) {
	std::vector<int> a{1, 2, 3};
	std::vector<int> b{1, 2, 3, 4, 5};
	std::vector<std::vector<int>> c{{1, 2, 3}};
	std::vector<std::vector<int>> d{{1, 2, 3},
									{4, 5}};
	EXPECT_EQ(contentHashCode(a), contentDeepHashCode(a));
	EXPECT_NE(0, contentDeepHashCode(a));
	EXPECT_NE(contentDeepHashCode(a), contentDeepHashCode(b));
	EXPECT_NE(contentDeepHashCode(a), contentDeepHashCode(c));
	EXPECT_NE(contentDeepHashCode(c), contentDeepHashCode(d));
	EXPECT_NE(contentDeepHashCode(b), contentDeepHashCode(d));
}

TEST(hash, contentDeepHashCodeOptional) {
	std::vector<tl::optional<int>> a{tl::nullopt, 2, tl::nullopt};
	std::vector<tl::optional<int>> b{tl::nullopt, 2, 3};

	EXPECT_NE(contentDeepHashCode(a), contentHashCode(b));
}
