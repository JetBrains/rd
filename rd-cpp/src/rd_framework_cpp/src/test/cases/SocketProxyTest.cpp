#include <gtest/gtest.h>


#include "SocketWireTestBase.h"
#include "impl/RdSignal.h"
#include "wire/SocketProxy.h"

using namespace rd;
using namespace rd::util;
using namespace test;
using namespace test::util;

TEST_F(SocketWireTestBase, TestSocketProxySimple) {
	auto proxyLifetimeDefinition = LifetimeDefinition(lifetime);
	auto proxyLifetime = proxyLifetimeDefinition.lifetime;
	auto serverProtocol = server(socketLifetime);

	sleep_this_thread(100);

	SocketProxy proxy("TestProxy", proxyLifetime, &serverProtocol);
	proxy.start();

	sleep_this_thread(100);

	auto clientProtocol = client(socketLifetime, proxy.getPort());

	sleep_this_thread(100);

	auto sp = RdSignal<int>();
	statics(sp, 1);
	sp.bind(lifetime, &serverProtocol, "Top");

	auto cp = RdSignal<int>();
	statics(cp, 1);
	cp.bind(lifetime, &clientProtocol, "Top");

	auto serverLog = std::vector<int>();
	auto clientLog = std::vector<int>();

	sp.advise(lifetime, [&serverLog](int i) {
		serverLog.push_back(i);
	});
	cp.advise(lifetime, [&clientLog](int i) {
		clientLog.push_back(i);
	});

	//Connection is established for now

	sp.fire(1);

	clientScheduler.pump_one_message();
	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{1}), serverLog);
	EXPECT_EQ((std::vector<int>{1}), clientLog);

	cp.fire(2);

	serverScheduler.pump_one_message();
	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{1, 2}), serverLog);
	EXPECT_EQ((std::vector<int>{1, 2}), clientLog);


	proxy.StopServerToClientMessaging();

	checkSchedulersAreEmpty();

	cp.advise(lifetime, [](int i) { ASSERT_NE(3, i); });

	sp.fire(3);

	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{1, 2, 3}), serverLog);


	proxy.StopClientToServerMessaging();

	checkSchedulersAreEmpty();

	sp.advise(lifetime, [](int i) { ASSERT_NE(4, i); });

	cp.fire(4);

	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{1, 2, 4}), clientLog);

	//Connection is broken for now

	proxy.StartServerToClientMessaging();
	checkSchedulersAreEmpty();

	sp.fire(5);

	clientScheduler.pump_one_message();
	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{1, 2, 3, 5}), serverLog);
	EXPECT_EQ((std::vector<int>{1, 2, 4, 5}), clientLog);


	proxy.StartClientToServerMessaging();

	cp.fire(6);

	serverScheduler.pump_one_message();
	checkSchedulersAreEmpty();

	EXPECT_EQ((std::vector<int>{1, 2, 3, 5, 6}), serverLog);
	EXPECT_EQ((std::vector<int>{1, 2, 4, 5, 6}), clientLog);

	//Connection is established for now

	proxyLifetimeDefinition.terminate();

	sp.fire(7);
	EXPECT_EQ((std::vector<int>{1, 2, 3, 5, 6, 7}), serverLog);

	cp.fire(8);

	EXPECT_EQ((std::vector<int>{1, 2, 4, 5, 6, 8}), clientLog);

	checkSchedulersAreEmpty();

	//Connection is broken for now, proxy is not alive

	terminate();
}
