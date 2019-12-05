#include <gtest/gtest.h>

#include "protocol/RdId.h"
#include "protocol/Identities.h"

using namespace rd;
using namespace rd::util;

TEST(rd_id, mix) {
	constexpr RdId id1 = RdId::Null().mix("abcd").mix("efg");
	RdId id2 = RdId::Null().mix("abcdefg");
	EXPECT_EQ(id1.get_hash(), id2.get_hash());
	EXPECT_EQ(id1.get_hash(), 88988021860L);
	RdId id3 = id2.mix("hijklmn");
	EXPECT_EQ(-5123855772550266649L, id3.get_hash());
	EXPECT_EQ(-5123855772550266649L * 31L + 1, id3.mix(0L).get_hash());
}

TEST(rd_id, constexprness) {
	constexpr auto hash = getPlatformIndependentHash("InternScopeInExt");
	EXPECT_EQ(-4122988489618686035L, hash);
}