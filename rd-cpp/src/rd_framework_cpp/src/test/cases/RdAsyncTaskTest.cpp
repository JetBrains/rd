#include "gtest/gtest.h"

#include "RdFrameworkTestBase.h"

#include "RdProperty.h"
#include "RdCall.h"
#include "RdEndpoint.h"

#include "Linearization.h"

using namespace rd;
using namespace rd::util;
using namespace test;

TEST_F(RdFrameworkTestBase, DISABLED_TestDynamic) {
	RdProperty<RdCall<int32_t, std::wstring> > client_property;
	RdProperty<RdEndpoint<int32_t, std::wstring> > server_property;

	statics(client_property, static_entity_id);
	statics(server_property, static_entity_id);
	server_property.slave();

	bindStatic(clientProtocol.get(), client_property, static_name);
	bindStatic(serverProtocol.get(), server_property, static_name);

	server_property.emplace([](int32_t x) -> std::wstring {
		return std::to_wstring(x);
	});

	EXPECT_EQ(L"1", client_property.get().sync(1).value_or_throw().unwrap());

	Linearization l;

	server_property.emplace([&l](Lifetime lifetime, int32_t const &v) -> RdTask<std::wstring> {
		RdTask<std::wstring> task;

		std::thread t([&l, task, v] {
			l.point(1);
			task.set(std::to_wstring(v));
			l.point(2);
		});
		t.detach();

		return task;
	});

	l.point(0);
	EXPECT_EQ(L"2", client_property.get().sync(2).value_or_throw().unwrap());
	l.point(3);
	l.reset();

	//wait for task
	auto task = client_property.get().start(3);
	EXPECT_FALSE(task.is_succeeded());
	EXPECT_FALSE(task.is_canceled());
	EXPECT_FALSE(task.is_faulted());
	EXPECT_FALSE(task.has_value());

	l.point(0);

	l.point(3);
	EXPECT_TRUE(task.is_succeeded());
	EXPECT_FALSE(task.is_canceled());
	EXPECT_FALSE(task.is_faulted());
	EXPECT_TRUE(task.has_value());

	l.reset();

	auto interrupted_task = client_property.get().start(0);

	//terminate request
	client_property.emplace();

	EXPECT_TRUE(interrupted_task.is_canceled());
	l.disable();

	AfterTest();
}