#include <gtest/gtest.h>

#include "impl/RdProperty.h"

#include "RdFrameworkTestBase.h"
#include "DynamicEntity.h"
#include "DynamicExt.h"
#include "SocketWireTestBase.h"
#include "ExtProperty.h"

using namespace rd;
using namespace test;
using namespace test::util;

// TEST_F(SocketWireTestBase, testStringExtension) {
//    Protocol serverProtocol = server(socketLifetime);
//    Protocol clientProtocol = client(socketLifetime, serverProtocol);
//
//    RdProperty<int32_t> sp{0}, cp{0};
//    cp.slave();
//
//    init(serverProtocol, clientProtocol, &sp, &cp);
//
//    sp.getOrCreateExtension<std::string>("data", []() { return "Immutable"; });
//
//    cp.getOrCreateExtension<std::string>("data", []() { return "Immutable"; });
//
//    sp.getOrCreateExtension<int>("data", []() { return int(1); }) = 2;
//
//    EXPECT_EQ(cp.get(), 0);
//    EXPECT_EQ(sp.get(), 0);
//
//    sp.set(1);
//    clientScheduler.pump_one_message();
//
//    std::string const &clientExt = cp.getOrCreateExtension<std::string>("data", []() { return "Mutable"; });
//    std::string const &serverExt = sp.getOrCreateExtension<std::string>("data", []() { return "Mutable"; });
//
//    checkSchedulersAreEmpty();
//
//    EXPECT_EQ(clientExt, "Immutable");
//    EXPECT_EQ(serverExt, "Immutable");
//
//    terminate();
//}

/*TEST_F(SocketWireTestBase, DISABLED_testExtension) {
	int property_id = 1;
	int entity_id = 2;
	int32_t foo_id = 3;

	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	RdProperty<DynamicEntity> clientProperty{DynamicEntity(0)};
	statics(clientProperty, property_id);
	statics(clientProperty.get(), entity_id);
	statics(clientProperty.get().foo, foo_id);

	RdProperty<DynamicEntity> serverProperty{DynamicEntity(0)};
	statics(serverProperty, property_id);
	statics(serverProperty.get(), entity_id);
	statics(serverProperty.get().foo, foo_id);

	serverProperty.slave();

	DynamicEntity::create(&clientProtocol);
	DynamicEntity::create(&serverProtocol);
	//bound
	clientProperty.bind(lifetime, &clientProtocol, static_name);
	serverProperty.bind(lifetime, &serverProtocol, static_name);

	DynamicEntity clientEntity(1);
	clientProperty.set(std::move(clientEntity));
	serverScheduler.pump_one_message();

	DynamicEntity serverEntity(1);
	serverProperty.set(std::move(serverEntity));
	clientScheduler.pump_one_message();

	//it's new!
	auto const &newServerEntity = serverProperty.get();

	DynamicExt const &serverExt = newServerEntity.getOrCreateExtension<DynamicExt>("ext", L"Ext!", L"client");

	clientScheduler.pump_one_message();
	//server send READY

	auto const &newClientEntity = clientProperty.get();
	DynamicExt const &clientExt = newClientEntity.getOrCreateExtension<DynamicExt>("ext", L"", L"server");

	serverScheduler.pump_one_message();
	//client send READY

	clientScheduler.pump_one_message();
	//server send COUNTERPART_ACK

	checkSchedulersAreEmpty();

	EXPECT_EQ(L"Ext!", serverExt.bar.get());
	EXPECT_EQ(L"Ext!", clientExt.bar.get());

	terminate();
}*/

TEST_F(SocketWireTestBase, /*DISABLED_*/ testSlowpokeExtension)
{
	//	int64_t const serialization_hash = 1ll << 40u;

	Protocol serverProtocol = server(socketLifetime);
	Protocol clientProtocol = client(socketLifetime, serverProtocol);

	RdProperty<int> serverProperty{0}, clientProperty{0};
	init(serverProtocol, clientProtocol, &serverProperty, &clientProperty);

	auto const& serverExt = serverProperty.getOrCreateExtension<ExtProperty<std::wstring>>("data", L"SERVER");
	//	serverExt.serializationHash = serialization_hash;

	serverExt.property.set(L"UPDATE");
	serverExt.property.set(L"UPGRADE");

	auto const& clientExt = clientProperty.getOrCreateExtension<ExtProperty<std::wstring>>("data", L"CLIENT");
	//	clientExt.serializationHash = serialization_hash;

	EXPECT_EQ(clientExt.property.get(), L"CLIENT");

	//	clientScheduler.pump_one_message(); //send Ready
	//	serverScheduler.pump_one_message(); //send Ready
	//	serverScheduler.pump_one_message(); //send ReceivedCounterpart
	//	clientScheduler.pump_one_message(); //send ReceivedCounterpart
	// no need in pumping due to synchronous scheduler
	clientScheduler.pump_one_message();	   // send "UPDATE"

	EXPECT_EQ(serverExt.property.get(), L"UPGRADE");
	EXPECT_EQ(clientExt.property.get(), L"UPDATE");

	clientScheduler.pump_one_message();	   // send "UPGRADE"
	checkSchedulersAreEmpty();

	EXPECT_EQ(serverExt.property.get(), L"UPGRADE");
	EXPECT_EQ(clientExt.property.get(), L"UPGRADE");

	terminate();
}
