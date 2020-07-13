#include <gtest/gtest.h>

#include "impl/RdMap.h"
#include "RdFrameworkTestBase.h"
#include "DynamicEntity.Generated.h"
#include "test_util.h"
#include "entities_util.h"

using namespace rd;
using namespace test;
using namespace test::util;

TEST_F(RdFrameworkTestBase, rd_map_statics)
{
	int32_t id = 1;

	RdMap<int32_t, std::wstring> server_map;
	RdMap<int32_t, std::wstring> client_map;

	statics(server_map, id);
	statics(client_map, id);

	server_map.optimize_nested = true;
	client_map.optimize_nested = true;

	std::vector<std::string> logUpdate;
	client_map.advise(Lifetime::Eternal(),
		[&](typename IViewableMap<int32_t, std::wstring>::Event entry) { logUpdate.push_back(to_string(entry)); });

	EXPECT_EQ(0, server_map.size());
	EXPECT_EQ(0, client_map.size());

	server_map.set(1, L"Server value 1");
	server_map.set(2, L"Server value 2");
	server_map.set(3, L"Server value 3");

	EXPECT_EQ(0, client_map.size());
	bindStatic(clientProtocol.get(), client_map, static_name);
	bindStatic(serverProtocol.get(), server_map, static_name);

	EXPECT_EQ(3, client_map.size());
	EXPECT_EQ(L"Server value 1", *client_map.get(1));
	EXPECT_EQ(L"Server value 2", *client_map.get(2));
	EXPECT_EQ(L"Server value 3", *client_map.get(3));
	EXPECT_EQ(nullptr, client_map.get(4));
	EXPECT_EQ(nullptr, client_map.get(4));

	server_map.set(4, L"Server value 4");
	client_map.set(4, L"Client value 4");

	EXPECT_EQ(L"Client value 4", *client_map.get(4));
	EXPECT_EQ(L"Client value 4", *server_map.get(4));

	client_map.set(5, L"Client value 5");
	server_map.set(5, L"Server value 5");

	EXPECT_EQ(L"Server value 5", *client_map.get(5));
	EXPECT_EQ(L"Server value 5", *server_map.get(5));

	EXPECT_EQ((std::vector<std::string>{"Add 1:Server value 1", "Add 2:Server value 2", "Add 3:Server value 3",
				  "Add 4:Server value 4", "Update 4:Client value 4", "Add 5:Client value 5", "Update 5:Server value 5"}),
		logUpdate);

	AfterTest();
}

TEST_F(RdFrameworkTestBase, rd_map_dynamic)
{
	int32_t id = 1;

	RdMap<int32_t, DynamicEntity> server_map;
	RdMap<int32_t, DynamicEntity> client_map;

	statics(server_map, id);
	statics(client_map, id);

	/*DynamicEntity::create(clientProtocol.get());
	DynamicEntity::create(serverProtocol.get());*/

	EXPECT_TRUE(server_map.empty());
	EXPECT_TRUE(client_map.empty());

	bindStatic(clientProtocol.get(), client_map, static_name);
	bindStatic(serverProtocol.get(), server_map, static_name);

	std::vector<std::wstring> log;
	server_map.view(Lifetime::Eternal(), [&](Lifetime lf, int32_t const& k, DynamicEntity const& v) {
		lf->bracket([&log, &k]() { log.push_back(L"start " + std::to_wstring(k)); },
			[&log, &k]() { log.push_back(L"finish " + std::to_wstring(k)); });
		v.get_foo().advise(lf, [&log](int32_t const& fooval) { log.push_back(std::to_wstring(fooval)); });
	});

	client_map.emplace_set(2, make_dynamic_entity(1));

	server_map.emplace_set(0, make_dynamic_entity(2));
	server_map.emplace_set(0, make_dynamic_entity(3));

	EXPECT_EQ(2, client_map.size());
	EXPECT_EQ(2, server_map.size());

	client_map.remove(0);
	client_map.emplace_set(5, make_dynamic_entity(4));

	client_map.clear();

	EXPECT_TRUE(client_map.empty());
	EXPECT_TRUE(server_map.empty());

	EXPECT_EQ((std::vector<std::wstring>{L"start 2", L"1", L"start 0", L"2", L"finish 0", L"start 0", L"3", L"finish 0", L"start 5",
				  L"4", L"finish 2", L"finish 5"}),
		log);

	AfterTest();
}

TEST_F(RdFrameworkTestBase, rd_map_move)
{
	RdMap<int, int> map1;
	RdMap<int, int> map2(std::move(map1));

	AfterTest();
}

TEST_F(RdFrameworkTestBase, map_iterator)
{
	RdMap<std::wstring, int> map;
	EXPECT_EQ(map.end(), map.rbegin().base());
	for (const auto& item : {1, 2, 3})
	{
		map.set(std::to_wstring(item), item);
	}
	EXPECT_EQ(map.end(), map.rbegin().base());
}