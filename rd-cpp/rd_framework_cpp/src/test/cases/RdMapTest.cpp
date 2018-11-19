//
// Created by jetbrains on 03.08.2018.
//

#include <gtest/gtest.h>

#include "RdMap.h"
#include "RdFrameworkTestBase.h"
#include "DynamicEntity.h"
//#include "test_util.h"
#include "../../../../rd_core_cpp/src/test/util/test_util.h"

TEST_F(RdFrameworkTestBase, rd_map_statics) {
    int32_t id = 1;

    RdMap<int32_t, std::string> server_map_storage;
    RdMap<int32_t, std::string> client_map_storage;

    RdMap<int32_t, std::string> &serverMap = statics(server_map_storage, id);
    RdMap<int32_t, std::string> &clientMap = statics(client_map_storage, id);

    server_map_storage.optimizeNested = true;
    client_map_storage.optimizeNested = true;

    std::vector<std::string> logUpdate;
    clientMap.advise(Lifetime::Eternal(), [&](typename IViewableMap<int32_t, std::string>::Event entry) {
        logUpdate.push_back(to_string_map_event<int32_t, std::string>(entry));
    });

    EXPECT_EQ(0, serverMap.size());
    EXPECT_EQ(0, clientMap.size());

    serverMap.set(1, "Server value 1");
    serverMap.set(2, "Server value 2");
    serverMap.set(3, "Server value 3");

    EXPECT_EQ(0, clientMap.size());
    bindStatic(clientProtocol.get(), clientMap, "top");
    bindStatic(serverProtocol.get(), serverMap, "top");

    EXPECT_EQ(3, clientMap.size());
    EXPECT_EQ("Server value 1", *clientMap.get(1));
    EXPECT_EQ("Server value 2", *clientMap.get(2));
    EXPECT_EQ("Server value 3", *clientMap.get(3));
    EXPECT_EQ(nullptr, clientMap.get(4));
    EXPECT_EQ(nullptr, clientMap.get(4));

    serverMap.set(4, "Server value 4");
    clientMap.set(4, "Client value 4");

    EXPECT_EQ("Client value 4", *clientMap.get(4));
    EXPECT_EQ("Client value 4", *serverMap.get(4));

    clientMap.set(5, "Client value 5");
    serverMap.set(5, "Server value 5");


    EXPECT_EQ("Server value 5", *clientMap.get(5));
    EXPECT_EQ("Server value 5", *serverMap.get(5));


    EXPECT_EQ((std::vector<std::string>{"Add 1:Server value 1",
                                        "Add 2:Server value 2",
                                        "Add 3:Server value 3",
                                        "Add 4:Server value 4",
                                        "Update 4:Client value 4",
                                        "Add 5:Client value 5",
                                        "Update 5:Server value 5"}),
              logUpdate
    );

    AfterTest();
}

TEST_F(RdFrameworkTestBase, rd_map_dynamic) {
    int32_t id = 1;

    RdMap<int32_t, DynamicEntity> server_map_storage;
    RdMap<int32_t, DynamicEntity> client_map_storage;

    RdMap<int32_t, DynamicEntity> &serverMap = statics(server_map_storage, id);
    RdMap<int32_t, DynamicEntity> &clientMap = statics(client_map_storage, id);

    DynamicEntity::create(clientProtocol.get());
    DynamicEntity::create(serverProtocol.get());

    EXPECT_TRUE(serverMap.empty());
    EXPECT_TRUE(clientMap.empty());

    bindStatic(clientProtocol.get(), clientMap, "top");
    bindStatic(serverProtocol.get(), serverMap, "top");

    std::vector<std::string> log;
    serverMap.view(Lifetime::Eternal(), [&](Lifetime lf, int32_t const &k, DynamicEntity const &v) {
        lf->bracket(
                [&log, &k]() { log.push_back("start " + std::to_string(k)); },
                [&log, &k]() { log.push_back("finish " + std::to_string(k)); }
        );
        v.foo.advise(lf, [&log](int32_t const &fooval) {
            log.push_back(std::to_string(fooval));
        });
    });

    clientMap.set(2, DynamicEntity(1));

    serverMap.set(0, DynamicEntity(2));
    serverMap.set(0, DynamicEntity(3));

    EXPECT_EQ(2, clientMap.size());
    EXPECT_EQ(2, serverMap.size());

    clientMap.remove(0);
    clientMap.set(5, DynamicEntity(4));

    clientMap.clear();

    EXPECT_TRUE(clientMap.empty());
    EXPECT_TRUE(serverMap.empty());

    EXPECT_EQ((std::vector<std::string>{"start 2", "1",
                                        "start 0", "2",
                                        "finish 0",
                                        "start 0", "3",
                                        "finish 0",
                                        "start 5", "4",
                                        "finish 2", "finish 5"}), log);

    AfterTest();
}
