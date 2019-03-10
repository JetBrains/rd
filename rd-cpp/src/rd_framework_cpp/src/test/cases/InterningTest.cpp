//
// Created by jetbrains on 3/1/2019.
//

#include <gtest/gtest.h>

#include "InterningTestBase.h"


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

//    val simpleTestData = simpleTestData

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