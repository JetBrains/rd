//
// Created by jetbrains on 01.11.2018.
//

#include "gtest/gtest.h"

#include <string>

#include "RdFrameworkTestBase.h"
#include "task/RdCall.h"
#include "task/RdEndpoint.h"

TEST_F(RdFrameworkTestBase, DISABLED_testStaticSuccess) {
    int entity_id = 1;

    auto client_entity = RdCall<int, std::string>();
    auto server_entity = RdEndpoint<int, std::string>([](int it) -> std::string { return to_string(it); });

    statics(client_entity, entity_id);
    statics(server_entity, entity_id);

    //not bound
//    EXPECT_THROW(client_entity.sync(0), std::exception);
//    EXPECT_THROW(client_entity.start(0), std::exception);

    //bound
    bindStatic(serverProtocol.get(), server_entity, "top");
    bindStatic(clientProtocol.get(), client_entity, "top");

    EXPECT_EQ("0", client_entity.sync(0));
    EXPECT_EQ("1", client_entity.sync(1));

    auto taskResult = client_entity.start(2).value_or_throw();
//    RdTaskResult.Success < String >);
//    assertEquals("2", taskResult.value);

    AfterTest();
}
