#include <gtest/gtest.h>

#include "impl/RdProperty.h"
#include "impl/RdList.h"
#include "RdFrameworkTestBase.h"
#include "DynamicEntity.Generated.h"
#include "test_util.h"
#include "entities_util.h"

using namespace rd;
using namespace test;
using namespace test::util;

TEST_F(RdFrameworkTestBase, rd_list_static)
{
	int32_t id = 1;

	RdList<std::wstring> server_list;
	RdList<std::wstring> client_list;

	statics(server_list, id);
	statics(client_list, id);

	server_list.optimize_nested = true;
	client_list.optimize_nested = true;

	std::vector<std::string> logUpdate;

	client_list.advise(
		Lifetime::Eternal(), [&](IViewableList<std::wstring>::Event entry) { logUpdate.emplace_back(to_string(entry)); });

	EXPECT_EQ(0, server_list.size());
	EXPECT_EQ(0, client_list.size());

	server_list.add(L"Server value 1");
	//    server_list.push_backAll(listOf("Server value 2", "Server value 3"));
	server_list.add(L"Server value 2");
	server_list.add(L"Server value 3");

	EXPECT_EQ(0, client_list.size());
	bindStatic(clientProtocol.get(), client_list, static_name);
	bindStatic(serverProtocol.get(), server_list, static_name);

	EXPECT_EQ(client_list.size(), 3);
	EXPECT_EQ(client_list.get(0), L"Server value 1");
	EXPECT_EQ(client_list.get(1), L"Server value 2");
	EXPECT_EQ(client_list.get(2), L"Server value 3");

	server_list.add(L"Server value 4");
	client_list.set(3, L"Client value 4");

	EXPECT_EQ(client_list.get(3), L"Client value 4");
	EXPECT_EQ(server_list.get(3), L"Client value 4");

	client_list.add(L"Client value 5");
	server_list.set(4, L"Server value 5");

	EXPECT_EQ(client_list.get(4), L"Server value 5");
	EXPECT_EQ(server_list.get(4), L"Server value 5");

	EXPECT_EQ(
		logUpdate, (std::vector<std::string>{"Add 0:Server value 1", "Add 1:Server value 2", "Add 2:Server value 3",
					   "Add 3:Server value 4", "Update 3:Client value 4", "Add 4:Client value 5", "Update 4:Server value 5"}));

	AfterTest();
}

TEST_F(RdFrameworkTestBase, rd_list_dynamic)
{
	int32_t id = 1;

	RdList<DynamicEntity> server_list;
	RdList<DynamicEntity> client_list;

	statics(server_list, id);
	statics(client_list, id);

	/*DynamicEntity::create(clientProtocol.get());
	DynamicEntity::create(serverProtocol.get());*/

	EXPECT_EQ(0, server_list.size());
	EXPECT_EQ(0, client_list.size());

	bindStatic(clientProtocol.get(), client_list, static_name);
	bindStatic(serverProtocol.get(), server_list, static_name);

	std::vector<std::string> log;

	server_list.view(Lifetime::Eternal(), [&](Lifetime lf, size_t k, DynamicEntity const& v) {
		lf->bracket([&log, k]() { log.push_back("start " + std::to_string(k)); },
			[&log, k]() { log.push_back("finish " + std::to_string(k)); });
		v.get_foo().advise(lf, [&log](int32_t const& fooval) { log.push_back(std::to_string(fooval)); });
	});

	client_list.emplace_add(make_dynamic_entity(2));
	client_list.get(0).get_foo().set(0);
	client_list.get(0).get_foo().set(0);

	client_list.get(0).get_foo().set(1);

	client_list.emplace_set(0, make_dynamic_entity(1));

	server_list.emplace_add(make_dynamic_entity(8));

	client_list.removeAt(1);
	client_list.emplace_add(make_dynamic_entity(3));

	client_list.clear();

	EXPECT_EQ(log, (std::vector<std::string>{"start 0", "2", "0", "1", "finish 0", "start 0", "1", "start 1", "8", "finish 1",
					   "start 1", "3", "finish 1", "finish 0"}));

	AfterTest();
}

/*TEST_F(RdFrameworkTestBase, rd_list_of_rd_property) {
	int32_t id = 1;

	RdList<RdProperty<int32_t>> server_list;
	RdList<RdProperty<int32_t>> client_list;

	statics(server_list, id);
	statics(client_list, id);

	EXPECT_EQ(0, server_list.size());
	EXPECT_EQ(0, client_list.size());

	bindStatic(clientProtocol.get(), client_list, static_name);
	bindStatic(serverProtocol.get(), server_list, static_name);

	std::vector<std::string> log;

	server_list.view(Lifetime::Eternal(), [&](Lifetime lf, size_t k, RdProperty<int32_t> const &v) {
		lf->bracket(
				[&log, k]() { log.push_back("start " + std::to_string(k)); },
				[&log, k]() { log.push_back("finish " + std::to_string(k)); }
		);
		v.advise(lf, [&log](int32_t const &val) { log.push_back(std::to_string(val)); });
	});


	server_list.add(RdProperty<int32_t>(0));

	client_list.add(RdProperty<int32_t>(0));

	client_list.set(0, RdProperty<int32_t>(2));

	server_list.add(RdProperty<int32_t>(1));

	server_list.add(RdProperty<int32_t>(8));

	client_list.clear();

	EXPECT_EQ(log, (std::vector<std::string>{"start 0", "0",
											 "start 1", "0",
											 "finish 0", "start 0", "2",
											 "start 2", "1",
											 "start 3", "8",
											 "finish 3",
											 "finish 2",
											 "finish 1",
											 "finish 0",
	}));

	AfterTest();
}*/

TEST_F(RdFrameworkTestBase, list_move)
{
	RdList<int> list1;
	RdList<int> list2(std::move(list1));

	AfterTest();
}

TEST_F(RdFrameworkTestBase, list_iterator)
{
	RdList<int> list;
	EXPECT_EQ(list.end(), list.rbegin().base());
	list.addAll({1, 2, 3});
	EXPECT_EQ(list.end(), list.rbegin().base());
}