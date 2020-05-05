#include "RdFrameworkTestBase.h"
#include "impl/RdSet.h"

#include <gtest/gtest.h>

using vi = std::vector<int>;

using namespace rd;
using namespace test;

TEST_F(RdFrameworkTestBase, set_statics)
{
	int32_t id = 1;

	RdSet<int> server_set;
	RdSet<int> client_set;

	statics(server_set, id);
	statics(client_set, id);

	vi log;

	server_set.advise(serverLifetimeDef.lifetime, [&](AddRemove kind, int v) { log.push_back((kind == AddRemove::ADD) ? v : -v); });

	client_set.add(2);
	client_set.add(0);
	client_set.add(1);
	client_set.add(8);

	EXPECT_EQ(vi(), log);

	bindStatic(serverProtocol.get(), server_set, static_name);
	bindStatic(clientProtocol.get(), client_set, static_name);
	EXPECT_EQ((vi{2, 0, 1, 8}), log);

	client_set.remove(1);
	EXPECT_EQ((vi{2, 0, 1, 8, -1}), log);

	server_set.remove(1);
	client_set.remove(1);
	EXPECT_EQ((vi{2, 0, 1, 8, -1}), log);

	client_set.remove(2);
	EXPECT_EQ((vi{2, 0, 1, 8, -1, -2}), log);

	client_set.clear();
	EXPECT_EQ((vi{2, 0, 1, 8, -1, -2, -0, -8}), log);

	AfterTest();
}

TEST_F(RdFrameworkTestBase, set_move)
{
	RdSet<int> set1;
	RdSet<int> set2(std::move(set1));

	AfterTest();
}

TEST_F(RdFrameworkTestBase, set_iterator)
{
	RdSet<int> set;
	EXPECT_EQ(set.end(), set.rbegin().base());
	set.addAll({1, 2, 3});
	EXPECT_EQ(set.end(), set.rbegin().base());
}