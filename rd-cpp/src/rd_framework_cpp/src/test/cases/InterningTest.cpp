//
// Created by jetbrains on 3/1/2019.
//

#include <gtest/gtest.h>

#include "InterningTestBase.h"
#include "InterningNestedTestStringModel.h"
#include "InterningNestedTestModel.h"
#include "PropertyHolderWithInternRoot.h"

using namespace rd;
using namespace rd::test;
using namespace rd::test::util;

TEST_F(InterningTestBase, testServerToClient) {
	doTest(false, false);
}

TEST_F(InterningTestBase, testClientToServer) {
	doTest(true, true);
}

TEST_F(InterningTestBase, testServerThenClientMixed) {
	doTest(false, true);
}

TEST_F(InterningTestBase, testClientThenServerMixed) {
	doTest(true, false);
}

TEST_F(InterningTestBase, testServerThenClientMixedAndReversed) {
	doTest(false, true, true);
}

TEST_F(InterningTestBase, testClientThenServerMixedAndReversed) {
	doTest(true, false, true);
}

TEST_F(InterningTestBase, testLateBindOfObjectWithContent) {
	auto serverProperty = RdProperty<InterningTestModel>();
	serverProperty.slave();
	auto clientProperty = RdProperty<InterningTestModel>();

	serverProperty.identify(*serverIdentities, RdId(1L));
	clientProperty.identify(*clientIdentities, RdId(1L));

	bindStatic(serverProtocol.get(), serverProperty, "top");
	bindStatic(clientProtocol.get(), clientProperty, "top");

	auto serverModel = InterningTestModel(L"");

	for_each([&](int32_t const &k, std::wstring const &v) {
		serverModel.get_issues().set(k, WrappedStringModel(v));
	});

	serverProperty.set(std::move(serverModel));

	auto const &clientModel = clientProperty.get();

	for_each([&](int32_t const &k, std::wstring const &v) {
		auto value = clientModel.get_issues().get(k);
		EXPECT_NE(nullptr, value);
		EXPECT_EQ(v, value->get_text());
	});


	AfterTest();
}

TEST_F(InterningTestBase, testServerToClientProtocolLevel) {
	testProtocolLevelIntern(false, false);
}

TEST_F(InterningTestBase, testClientToServerProtocolLevel) {
	testProtocolLevelIntern(true, true);
}

TEST_F(InterningTestBase, testServerThenClientMixedProtocolLevel) {
	testProtocolLevelIntern(false, true);
}

TEST_F(InterningTestBase, testClientThenServerMixedProtocolLevel) {
	testProtocolLevelIntern(true, false);
}

TEST_F(InterningTestBase, testServerThenClientMixedAndReversedProtocolLevel) {
	testProtocolLevelIntern(false, true, true);
}

TEST_F(InterningTestBase, testClientThenServerMixedAndReversedProtocolLevel) {
	testProtocolLevelIntern(true, false, true);
}

TEST_F(InterningTestBase, testNestedInternedObjects) {
	//todo rewrite it from kotlin side
}

TEST_F(InterningTestBase, testNestedInternedObjectsOnSameData) {
	serverProtocol->get_serializers().registry<InterningNestedTestModel>();
	clientProtocol->get_serializers().registry<InterningNestedTestModel>();

	RdProperty<InterningNestedTestStringModel> server_property;
	RdProperty<InterningNestedTestStringModel> client_property;

	statics(server_property, 1).slave();
	statics(client_property, 1);

	PropertyHolderWithInternRoot<InterningNestedTestStringModel> serverPropertyHolder(std::move(server_property));
	PropertyHolderWithInternRoot<InterningNestedTestStringModel> clientPropertyHolder(std::move(client_property));

	serverPropertyHolder.mySerializationContext =
			serverProtocol->get_serialization_context().withInternRootsHere(serverPropertyHolder, {"TestInternScope"});
	clientPropertyHolder.mySerializationContext =
			clientProtocol->get_serialization_context().withInternRootsHere(clientPropertyHolder, {"TestInternScope"});

	auto const &server_property_view = serverPropertyHolder.property;
	auto const &client_property_view = clientPropertyHolder.property;

	bindStatic(serverProtocol.get(), serverPropertyHolder, "top");
	bindStatic(clientProtocol.get(), clientPropertyHolder, "top");

	std::wstring sameString = L"thisStringHasANiceLengthThatWillDominateBytesSentCount";
	auto nested0 = wrapper::make_wrapper<InterningNestedTestStringModel>(sameString, Wrapper<InterningNestedTestStringModel>(nullptr));
	auto nested1 = wrapper::make_wrapper<InterningNestedTestStringModel>(sameString, *nested0);
	auto testValue = wrapper::make_wrapper<InterningNestedTestStringModel>(sameString, *nested1);
	//todo wrapper nullopt

	auto firstSendBytes = measureBytes(serverProtocol.get(), [&]() {
		server_property_view.set(*testValue);
		EXPECT_EQ(*testValue, client_property_view.get());
	});

	// expected send: string + 4 bytes length + 4 bytes id + 8+4 bytes polymorphic write, 3 bytes nullability, 3x 4byte ids, 4 bytes property version
	int32_t sendTarget = sameString.length() * 2 + 4 + 4 + 8 + 4 + 3 + 4 * 3 + 4;
	MY_ASSERT_MSG(firstSendBytes <= sendTarget, "Sent " + std::to_string(firstSendBytes) + ", expected " + std::to_string(sendTarget))

	AfterTest();
}
