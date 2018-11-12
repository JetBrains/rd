//
// Created by jetbrains on 01.11.2018.
//

#include "gtest/gtest.h"

#include <string>

#include "RdFrameworkTestBase.h"
#include "RdCall.h"
#include "RdEndpoint.h"

TEST_F(RdFrameworkTestBase, testStaticSuccess) {
    int entity_id = 1;

    auto client_entity = RdCall<int, std::string>();
    auto server_entity = RdEndpoint<int, std::string>([](int const &it) -> std::string { return to_string(it); });

    statics(client_entity, entity_id);
    statics(server_entity, entity_id);

    //not bound
    EXPECT_THROW(client_entity.sync(0), std::exception);
    EXPECT_THROW(client_entity.start(0), std::exception);

    //bound
    bindStatic(serverProtocol.get(), server_entity, "top");
    bindStatic(clientProtocol.get(), client_entity, "top");

    EXPECT_EQ("0", client_entity.sync(0));
    EXPECT_EQ("1", client_entity.sync(1));

    auto taskResult = client_entity.start(2).value_or_throw();
    EXPECT_EQ("2", taskResult.unwrap());

    AfterTest();
}

TEST_F(RdFrameworkTestBase, testStaticDifficult) {
    int entity_id = 1;

    auto client_entity = RdCall<std::string, int64_t>();
    auto server_entity = RdEndpoint<std::string, int64_t>(
            [](std::string const &s) -> int64_t { return std::hash<std::string>()(s); });

    statics(client_entity, entity_id);
    statics(server_entity, entity_id);

    bindStatic(serverProtocol.get(), server_entity, "top");
    bindStatic(clientProtocol.get(), client_entity, "top");

    std::string source(10'000'000, '5');

    EXPECT_EQ(std::hash<std::string>()(source), client_entity.sync(source));

    AfterTest();
}

TEST_F(RdFrameworkTestBase, testStaticFailure) {
    int entity_id = 1;

    auto client_entity = RdCall<int, std::string>();
    auto server_entity = RdEndpoint<int, std::string>([](int) -> std::string { throw std::runtime_error("1234"); });

    statics(client_entity, entity_id);
    statics(server_entity, entity_id);

    bindStatic(serverProtocol.get(), server_entity, "top");
    bindStatic(clientProtocol.get(), client_entity, "top");

    auto task = client_entity.start(2);
    EXPECT_TRUE(task.isFaulted());

    auto taskResult = (task.value_or_throw());
//    assertEquals("1234", taskResult.error.reasonMessage)
//    assertEquals("IllegalStateException", taskResult.error.reasonTypeFqn)
}
