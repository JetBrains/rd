//
// Created by jetbrains on 02.08.2018.
//

#include <gtest/gtest.h>

#include "RdSet.h"
#include "RdFrameworkTestBase.h"

using vi = std::vector<int>;

using namespace rd;
using namespace test;
using namespace test::util;

TEST_F(RdFrameworkTestBase, set_statics) {
    int32_t id = 1;

    RdSet<int> server_set_storage;
    RdSet<int> client_server_set_storage;

    RdSet<int> &serverSet = statics(server_set_storage, id);
    RdSet<int> &clientSet = statics(client_server_set_storage, id);

    vi log;

    serverSet.advise(serverLifetimeDef.lifetime,
                     [&](AddRemove kind, int v) {
                         log.push_back((kind == AddRemove::ADD) ? v :
                                       -v);
                     });

    clientSet.add(2);
    clientSet.add(0);
    clientSet.add(1);
    clientSet.add(8);


    EXPECT_EQ(vi(), log);

    bindStatic(serverProtocol.get(), serverSet, "top");
    bindStatic(clientProtocol.get(), clientSet, "top");
    EXPECT_EQ((vi{2, 0, 1, 8}), log);

    clientSet.remove(1);
    EXPECT_EQ((vi{2, 0, 1, 8, -1}), log);

    serverSet.remove(1);
    clientSet.remove(1);
    EXPECT_EQ((vi{2, 0, 1, 8, -1}), log);

    clientSet.remove(2);
    EXPECT_EQ((vi{2, 0, 1, 8, -1, -2}), log);


    clientSet.clear();
    EXPECT_EQ((vi{2, 0, 1, 8, -1, -2, -0, -8}), log);

    AfterTest();
}

TEST_F(RdFrameworkTestBase, set_move) {
    RdSet<int> set1;
    RdSet<int> set2(std::move(set1));
}