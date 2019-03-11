//
// Created by jetbrains on 01.11.2018.
//

#include "gtest/gtest.h"

#include "RdFrameworkTestBase.h"
#include "RdCall.h"
#include "RdEndpoint.h"

#include <string>

using namespace rd;
using namespace test;

TEST_F(RdFrameworkTestBase, testStaticSuccess) {
	int entity_id = 1;

	RdCall<int, std::wstring> client_entity;
	RdEndpoint<int, std::wstring> server_entity([](int const &it) -> std::wstring { return std::to_wstring(it); });

	statics(client_entity, entity_id);
	statics(server_entity, entity_id);

	//not bound
	EXPECT_THROW(client_entity.sync(0), std::exception);
	EXPECT_THROW(client_entity.start(0), std::exception);

	//bound
	bindStatic(serverProtocol.get(), server_entity, "top");
	bindStatic(clientProtocol.get(), client_entity, "top");

	EXPECT_EQ(L"0", client_entity.sync(0));
	EXPECT_EQ(L"1", client_entity.sync(1));

	auto taskResult = client_entity.start(2).value_or_throw();
	EXPECT_EQ(L"2", taskResult.unwrap());

	AfterTest();
}

TEST_F(RdFrameworkTestBase, testStaticDifficult) {
	int entity_id = 1;

	auto client_entity = RdCall<std::wstring, int64_t>();
	auto server_entity = RdEndpoint<std::wstring, int64_t>(
			[](std::wstring const &s) -> int64_t { return std::hash<std::wstring>()(s); });

	statics(client_entity, entity_id);
	statics(server_entity, entity_id);

	bindStatic(serverProtocol.get(), server_entity, "top");
	bindStatic(clientProtocol.get(), client_entity, "top");

	std::wstring source(10'000, '5');

	EXPECT_EQ(std::hash<std::wstring>()(source), client_entity.sync(source));

	AfterTest();
}

TEST_F(RdFrameworkTestBase, testStaticFailure) {
	int entity_id = 1;

	auto client_entity = RdCall<int, std::wstring>();
	auto server_entity = RdEndpoint<int, std::wstring>([](int) -> std::wstring { throw std::runtime_error("1234"); });

	statics(client_entity, entity_id);
	statics(server_entity, entity_id);

	bindStatic(serverProtocol.get(), server_entity, "top");
	bindStatic(clientProtocol.get(), client_entity, "top");

	auto task = client_entity.start(2);
	EXPECT_TRUE(task.isFaulted());

	auto taskResult = task.value_or_throw();

//    EXPECT_EQ("1234", taskResult.error.reasonMessage);
//    EXPECT_EQ("IllegalStateException", taskResult.error.reasonTypeFqn);
	AfterTest();
}
