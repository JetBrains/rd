//
// Created by jetbrains on 20.07.2018.
//

#include <gtest/gtest.h>

#include "RdId.h"

TEST(rd_id, mix) {
    RdId id1 = RdId::Null().mix("abcd").mix("efg");
    RdId id2 = RdId::Null().mix("abcdefg");
    EXPECT_EQ(id1.get_hash(), id2.get_hash());
    EXPECT_EQ(id1.get_hash(), 88988021860L);
}