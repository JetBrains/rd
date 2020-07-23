#include <gtest/gtest.h>

#include "RdFrameworkTestBase.h"
#include "task/RdCall.h"
#include "task/RdEndpoint.h"
#include "task/RdSymmetricCall.h"

#include <string>

using namespace rd;
using namespace test;

TEST_F(RdFrameworkTestBase, testStaticSuccess)
{
	int entity_id = 1;

	RdCall<int, std::wstring> client_entity;
	RdEndpoint<int, std::wstring> server_entity([](int const& it) -> std::wstring { return std::to_wstring(it); });

	statics(client_entity, entity_id);
	statics(server_entity, entity_id);

	// not bound
	EXPECT_THROW(client_entity.sync(0), std::invalid_argument);
	EXPECT_THROW(client_entity.start(0), std::invalid_argument);

	// bound
	bindStatic(serverProtocol.get(), server_entity, static_name);
	bindStatic(clientProtocol.get(), client_entity, static_name);

	EXPECT_EQ(L"0", client_entity.sync(0).value_or_throw().unwrap());
	EXPECT_EQ(L"1", client_entity.sync(1).value_or_throw().unwrap());

	auto taskResult = client_entity.start(2).value_or_throw();
	EXPECT_EQ(L"2", taskResult.unwrap());

	AfterTest();
}

TEST_F(RdFrameworkTestBase, testStaticDifficult)
{
	int entity_id = 1;

	auto client_entity = RdCall<std::wstring, int64_t>();
	auto server_entity =
		RdEndpoint<std::wstring, int64_t>([](std::wstring const& s) -> int64_t { return std::hash<std::wstring>()(s); });

	statics(client_entity, entity_id);
	statics(server_entity, entity_id);

	bindStatic(serverProtocol.get(), server_entity, static_name);
	bindStatic(clientProtocol.get(), client_entity, static_name);

	std::wstring source(10'000, '5');

	EXPECT_EQ(std::hash<std::wstring>()(source), client_entity.sync(source).value_or_throw().unwrap());

	AfterTest();
}

TEST_F(RdFrameworkTestBase, testStaticFailure)
{
	auto client_entity = RdCall<int, std::wstring>();
	auto server_entity = RdEndpoint<int, std::wstring>([](int) -> std::wstring { throw std::runtime_error("1234"); });

	statics(client_entity, static_entity_id);
	statics(server_entity, static_entity_id);

	bindStatic(serverProtocol.get(), server_entity, static_name);
	bindStatic(clientProtocol.get(), client_entity, static_name);

	auto task = client_entity.start(2);
	EXPECT_TRUE(task.is_faulted());

	RdTaskResult<std::wstring> task_result = task.value_or_throw();

	task_result.as_faulted([&](typename decltype(task_result)::Fault const& t) { EXPECT_EQ(L"1234", t.reason_message); });

	AfterTest();
}

TEST_F(RdFrameworkTestBase, testSymmetricCall)
{
	RdSymmetricCall<std::wstring, int32_t> server_entity, client_entity;

	statics(client_entity, static_entity_id);
	statics(server_entity, static_entity_id);

	server_entity.set([](std::wstring const& s) { return +static_cast<int32_t>(s.length()); });
	client_entity.set([](std::wstring const& s) { return -static_cast<int32_t>(s.length()); });

	bindStatic(serverProtocol.get(), server_entity, static_name);
	bindStatic(clientProtocol.get(), client_entity, static_name);

	EXPECT_EQ(+2, client_entity.sync(L"ab").value_or_throw().unwrap());
	EXPECT_EQ(-2, server_entity.sync(L"xy").value_or_throw().unwrap());

	AfterTest();
}