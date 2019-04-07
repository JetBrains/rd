#include <gtest/gtest.h>

#include "RdMap.h"
#include "Protocol.h"
#include "RdProperty.h"
#include "SocketWireTestBase.h"
#include "SocketWire.h"
#include "DynamicEntity.h"

#include <random>

const int STEP = 5;

using namespace rd;
using namespace rd::util;
using namespace test;
using namespace test::util;

TEST_F(SocketWireTestBase, ClientWithoutServer) {
	uint16_t port = find_free_port();
	Protocol protocol = client(socketLifetime, port);

	terminate();
}

TEST_F(SocketWireTestBase, ServerWithoutClient) {
	Protocol protocol = server(socketLifetime);

	terminate();
}

TEST_F(SocketWireTestBase, TestServerWithoutClientWithDelay) {
	Protocol protocol = server(socketLifetime);

	sleep_this_thread(100);

	terminate();
}

TEST_F(SocketWireTestBase, TestClientWithoutServerWithDelay) {
	uint16_t port = find_free_port();
	auto protocol = client(socketLifetime, port);

	sleep_this_thread(100);

	terminate();
}

TEST_F(SocketWireTestBase, /*DISABLED_*/TestServerWithoutClientWithDelayAndMessages) {
	auto protocol = server(socketLifetime);

	sleep_this_thread(100);

	RdProperty<int> sp(0);
	statics(sp, 1);
	sp.bind(lifetime, &protocol, "top");

	sp.set(1);

	sp.set(2);
	sleep_this_thread(50);

	terminate();
}

TEST_F(SocketWireTestBase, /*DISABLED_*/TestClientWithoutServerWithDelayAndMessages) {
	uint16_t port = find_free_port();
	auto clientProtocol = client(socketLifetime, port);

	RdProperty<int> cp(0);
	statics(cp, 1);
	cp.bind(lifetime, &clientProtocol, "top");

	cp.set(1);
	cp.set(2);

	sleep_this_thread(50);

	terminate();
}

TEST_F(SocketWireTestBase, TestBasicEmptyRun) {
	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	init(serverProtocol, clientProtocol);

	for (int i = 0; i < 10; ++i) {
		sleep_this_thread(100);
	}

	terminate();
}

TEST_F(SocketWireTestBase, TestBasicRun) {
	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	RdProperty<int> sp{0}, cp{0};

	init(serverProtocol, clientProtocol, &sp, &cp);

	cp.set(1);

	serverScheduler.pump_one_message(); //server get new value

	checkSchedulersAreEmpty();

	EXPECT_EQ(sp.get(), 1);

	sp.set(2);
	clientScheduler.pump_one_message(); //server get new value

	checkSchedulersAreEmpty();

	EXPECT_EQ(cp.get(), 2);

	terminate();
}

TEST_F(SocketWireTestBase, TestOrdering) {
	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	RdProperty<int> sp{0}, cp{0};

	init(serverProtocol, clientProtocol, &sp, &cp);

	std::vector<int> log;//concurrent?
	std::mutex lock;
	sp.advise(lifetime, [&](const int &it) {
		std::lock_guard<decltype(lock)> guard(lock);
		log.push_back(it);
	});
	for (int i = 1; i <= STEP; ++i) {
		cp.set(i);
		serverScheduler.pump_one_message();//server get new value
	}

	while (true) {
		bool x;
		{
			std::lock_guard<decltype(lock)> guard(lock);
			x = log.size() < 6;
		}
		if (x) {
			sleep_this_thread(100);
		} else {
			break;
		}
	}

	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{0, 1, 2, 3, 4, 5}), log);

	terminate();
}

TEST_F(SocketWireTestBase, TestBigBuffer) {
	RdProperty<std::wstring> cp_string{L""}, sp_string{L""};

	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	statics(sp_string, property_id);
	sp_string.bind(lifetime, &serverProtocol, "top");

	statics(cp_string, property_id);
	cp_string.bind(lifetime, &clientProtocol, "top");

	cp_string.set(L"1");

	serverScheduler.pump_one_message();//server gets new small string

	checkSchedulersAreEmpty();

	EXPECT_EQ(sp_string.get(), L"1");

	std::wstring str(100'000, '3');
	sp_string.set(str);
	clientScheduler.pump_one_message();//client gets new big string

	checkSchedulersAreEmpty();

	EXPECT_EQ(cp_string.get(), str);

	terminate();
}

TEST_F(SocketWireTestBase, TestComplicatedProperty) {
	using listOf = std::vector<int32_t>;

	int property_id = 1;

	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	RdProperty<DynamicEntity> client_property{DynamicEntity(0)}, server_property{DynamicEntity(0)};

	statics(client_property, (property_id));
	statics(server_property, (property_id)).slave();

	client_property.get().rdid = server_property.get().rdid = RdId(2);
	client_property.get().foo.rdid = server_property.get().foo.rdid = RdId(3);

	DynamicEntity::create(&clientProtocol);
	DynamicEntity::create(&serverProtocol);
	//bound

	server_property.bind(lifetime, &serverProtocol, "top");
	client_property.bind(lifetime, &clientProtocol, "top");

	std::vector<int32_t> clientLog;
	std::vector<int32_t> serverLog;

	client_property.advise(Lifetime::Eternal(), [&](DynamicEntity const &entity) {
		entity.foo.advise(Lifetime::Eternal(), [&](int32_t const &it) { clientLog.push_back(it); });
	});
	server_property.advise(Lifetime::Eternal(), [&](DynamicEntity const &entity) {
		entity.foo.advise(Lifetime::Eternal(), [&](int32_t const &it) { serverLog.push_back(it); });
	});

	checkSchedulersAreEmpty();

	EXPECT_EQ((listOf{0}), clientLog);
	EXPECT_EQ((listOf{0}), serverLog);

	client_property.set(DynamicEntity(2));
	serverScheduler.pump_one_message();//server get the whole DynamicEntity in one message

	checkSchedulersAreEmpty();

	EXPECT_EQ(clientLog, (listOf{0, 2}));
	EXPECT_EQ(serverLog, (listOf{0, 2}));

	client_property.get().foo.set(5);
	serverScheduler.pump_one_message();//server get the only foo in one message

	checkSchedulersAreEmpty();

	EXPECT_EQ(clientLog, (listOf{0, 2, 5}));
	EXPECT_EQ(serverLog, (listOf{0, 2, 5}));

	client_property.get().foo.set(5);

	checkSchedulersAreEmpty();

	EXPECT_EQ(clientLog, (listOf{0, 2, 5}));
	EXPECT_EQ(serverLog, (listOf{0, 2, 5}));

	client_property.set(DynamicEntity(5));
	serverScheduler.pump_one_message();//server get the whole DynamicEntity in one message

	checkSchedulersAreEmpty();

	EXPECT_EQ(clientLog, (listOf{0, 2, 5, 5}));
	EXPECT_EQ(serverLog, (listOf{0, 2, 5, 5}));

	terminate();
}

TEST_F(SocketWireTestBase, TestEqualChangesRdMap) { //Test pending for ack
	auto serverProtocol = server(socketLifetime);
	auto clientProtocol = client(socketLifetime, serverProtocol);

	RdMap<std::wstring, std::wstring> s_map, c_map;
	s_map.master = true;
	init(serverProtocol, clientProtocol, &s_map, &c_map);

	s_map.set(L"A", L"B");
	clientScheduler.pump_one_message();//client get ADD and send ACK
	serverScheduler.pump_one_message();//server get ACK
	for (int i = 0; i < STEP; ++i) {
		s_map.set(L"A", L"B");
	}
	c_map.set(L"A", L"B");

	checkSchedulersAreEmpty();

	EXPECT_EQ(*s_map.get(L"A"), L"B");
	EXPECT_EQ(*c_map.get(L"A"), L"B");

	terminate();
}

TEST_F(SocketWireTestBase, TestDifferentChangesRdMap) { //Test pending for ack
	auto serverProtocol = server(socketLifetime);
	auto clientProtocol = client(socketLifetime, serverProtocol);

	RdMap<std::wstring, std::wstring> s_map, c_map;
	s_map.master = true;
	init(serverProtocol, clientProtocol, &s_map, &c_map);

	s_map.set(L"A", L"B");
	clientScheduler.pump_one_message();//client get ADD and send ACK
	serverScheduler.pump_one_message();//server get ACK
	for (int i = 0; i < STEP; ++i) {
		s_map.set(L"A", L"B");
	}

	c_map.set(L"A", L"C");
	serverScheduler.pump_one_message();//server get ADD
	for (int i = 0; i < STEP; ++i) {
		c_map.set(L"A", L"C");
	}

	checkSchedulersAreEmpty();

	EXPECT_EQ(*s_map.get(L"A"), L"C");
	EXPECT_EQ(*c_map.get(L"A"), L"C");

	terminate();
}

TEST_F(SocketWireTestBase, TestPingPongRdMap) { //Test pending for ack
	srand(0);

	auto serverProtocol = server(socketLifetime);
	auto clientProtocol = client(socketLifetime, serverProtocol);

	RdMap<std::wstring, int> s_map, c_map;
	s_map.master = true;
	init(serverProtocol, clientProtocol, &s_map, &c_map);

	std::vector<int> list(STEP);
	int number = 0;
	std::generate_n(list.begin(), STEP, [&number]() { return ++number; });
	std::shuffle(list.begin(), list.end(), std::mt19937(std::random_device()()));

	bool f = true;
	for (auto x : list) {
		if (f) {
			s_map.set(L"A", x);
			clientScheduler.pump_one_message();//client get ADD and send ACK
			serverScheduler.pump_one_message();//server get ACK
		} else {
			c_map.set(L"A", x);
			serverScheduler.pump_one_message();//server get ADD and doesn't send ACK
		}
		checkSchedulersAreEmpty();

		EXPECT_EQ(*s_map.get(L"A"), x);
		EXPECT_EQ(*c_map.get(L"A"), x);

		f = !f;
	}

	int last = *list.rbegin();

	checkSchedulersAreEmpty();

	EXPECT_EQ(*s_map.get(L"A"), last);
	EXPECT_EQ(*c_map.get(L"A"), last);

	terminate();
}

TEST_F(SocketWireTestBase, /*DISABLED_*/TestRunWithSlowpokeServer) {
	uint16_t port = find_free_port();
	auto clientProtocol = client(socketLifetime, port);

	RdProperty<int> sp{0}, cp{0};

	statics(cp, property_id);
	cp.bind(lifetime, &clientProtocol, "top");

	cp.set(1);

	sleep_this_thread(2000);

	auto serverProtocol = server(socketLifetime, port);

	statics(sp, property_id);
	sp.bind(lifetime, &serverProtocol, "top");

	cp.set(4);
	serverScheduler.pump_one_message();
	serverScheduler.pump_one_message();

	checkSchedulersAreEmpty();

	EXPECT_EQ(sp.get(), 4);

	terminate();
}

TEST_F(SocketWireTestBase, DISABLED_failoverServer) {
	uint16_t port = find_free_port();
	auto serverProtocol = server(socketLifetime, port);

	LifetimeDefinition unstableLifetimeDef{Lifetime::Eternal()};
	Lifetime unstableLifetime = unstableLifetimeDef.lifetime;

	auto clientProtocol = client(unstableLifetime, port);

	RdProperty<int> sp{0}, cp{0};
	statics(cp, property_id);
	statics(sp, property_id);
	sp.bind(lifetime, &serverProtocol, "top");
	cp.bind(lifetime, &clientProtocol, "top");

	sp.set(1);

	clientScheduler.pump_one_message(); //send 1

	EXPECT_EQ(1, sp.get());
	EXPECT_EQ(1, cp.get());


	unstableLifetimeDef.terminate();

	sp.set(2);


	auto rebornClientProtocol = client(socketLifetime, port);
	RdProperty<int> np;
	statics(np, property_id);
	np.bind(lifetime, &rebornClientProtocol, "top");

	checkSchedulersAreEmpty();
	// clientScheduler.pump_one_message(); //send 2
	// EXPECT_EQ(2, np.get());

	terminate();
}
