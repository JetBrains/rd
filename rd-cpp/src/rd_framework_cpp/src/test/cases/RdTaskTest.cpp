#include <gtest/gtest.h>

#include "RdFrameworkTestBase.h"
#include "DynamicExt/ConcreteEntity.Generated.h"
#include "DynamicExt/DynamicEntity.Generated.h"
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

TEST_F(RdFrameworkTestBase, testBindableCall)
{
	RdEndpoint<std::wstring, test::util::DynamicEntity> server_entity;
	RdCall<std::wstring, test::util::DynamicEntity> client_entity;

	statics(server_entity, static_entity_id);
	statics(client_entity, static_entity_id);

	Wrapper<test::util::DynamicEntity> server_result;
	bool server_result_lifetime_terminated = false;

	server_entity.set([&](const Lifetime& lifetime, std::wstring const& s)
	{
		server_result = wrapper::make_wrapper<test::util::DynamicEntity>();
		server_result->get_foo().set(static_cast<int32_t>(s.length()));
		lifetime->add_action([&] { server_result_lifetime_terminated = true; });
		return server_result;
	});

	bindStatic(serverProtocol.get(), server_entity, static_name);
	bindStatic(clientProtocol.get(), client_entity, static_name);

	RdId property_id;
	{
		auto client_result = client_entity.start(L"xy").value_or_throw().get_value();
		property_id = dynamic_cast<const RdProperty<int32_t>*>(&client_result->get_foo())->get_id();
		EXPECT_EQ(2, client_result->get_foo().get()) << "Expected client result to recieve value from server.";

		EXPECT_TRUE(serverWire->is_subscribed(property_id)) << "Expected to auto-bind server result";
		EXPECT_TRUE(clientWire->is_subscribed(property_id)) << "Expected to auto-bind client result";

		server_result->get_foo().set(42);
		EXPECT_EQ(42, client_result->get_foo().get()) << "Expected client result to be auto-binded to server result.";
		// client_result leaves scopes here and should release all resources
	}

	EXPECT_FALSE(serverWire->is_subscribed(property_id)) << "Expected to auto-unbind server result";
	EXPECT_FALSE(clientWire->is_subscribed(property_id)) << "Expected to auto-unbind client result";

	EXPECT_TRUE(server_result_lifetime_terminated) << "Expected server lifetime for result to be terminated.";
	EXPECT_TRUE(server_result.unique()) << "Expected server_result to be released. Test should hold only reference to server result.";

	AfterTest();
}

TEST_F(RdFrameworkTestBase, testAsyncBindableCall)
{
	RdEndpoint<std::wstring, test::util::DynamicEntity> server_entity;
	RdCall<std::wstring, test::util::DynamicEntity> client_entity;

	statics(server_entity, static_entity_id);
	statics(client_entity, static_entity_id);

	bindStatic(serverProtocol.get(), server_entity, static_name);
	bindStatic(clientProtocol.get(), client_entity, static_name);

	Wrapper<test::util::DynamicEntity> server_result;
	bool server_result_lifetime_terminated = false;
	RdId property_id;

	{
		Wrapper<test::util::DynamicEntity> client_result;

		{
			RdTask<test::util::DynamicEntity> server_result_task{};
			std::wstring req;

			server_entity.set(
				[&](const Lifetime& lifetime, std::wstring const& s)
				{
					req = s;
					lifetime->add_action([&] { server_result_lifetime_terminated = true; });
					return server_result_task;
				});

			auto client_result_task = client_entity.start(L"xy");
			EXPECT_THROW(client_result_task.value_or_throw(), std::exception);
			EXPECT_EQ(req, L"xy");

			server_result = wrapper::make_wrapper<test::util::DynamicEntity>();
			server_result->get_foo().set(static_cast<int32_t>(req.length()));
			server_result_task.set(server_result);

			client_result = client_result_task.value_or_throw().get_value();
			// release tasks, but preserve results
		}

		property_id = dynamic_cast<const RdProperty<int32_t>*>(&client_result->get_foo())->get_id();
		EXPECT_EQ(2, client_result->get_foo().get()) << "Expected client result to recieve value from server.";

		EXPECT_TRUE(serverWire->is_subscribed(property_id)) << "Expected to auto-bind server result";
		EXPECT_TRUE(clientWire->is_subscribed(property_id)) << "Expected to auto-bind client result";

		server_result->get_foo().set(42);
		EXPECT_EQ(42, client_result->get_foo().get()) << "Expected client result to be auto-binded to server result.";
		// client_result leaves scopes here and should release all resources
	}

	EXPECT_FALSE(serverWire->is_subscribed(property_id)) << "Expected to auto-unbind server result";
	EXPECT_FALSE(clientWire->is_subscribed(property_id)) << "Expected to auto-unbind client result";

	EXPECT_TRUE(server_result_lifetime_terminated) << "Expected server lifetime for result to be terminated.";
	EXPECT_TRUE(server_result.unique()) << "Expected server_result to be released. Test should hold only reference to server result.";

	AfterTest();
}