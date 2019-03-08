//
// Created by jetbrains on 3/1/2019.
//

#include <gtest/gtest.h>

#include "InterningTestBase.h"

#include "InterningTestModel.h"
#include "RdProperty.h"

#include <cstdint>
#include <models/InterningRoot1/InterningTestModel.h>


using namespace rd;
using namespace rd::test;
using namespace rd::test::util;

/*TEST_F(InterningTestBase, testServerToClient) {
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
}*/

TEST_F(InterningTestBase, testLateBindOfObjectWithContent) {
    auto serverProperty = RdProperty<InterningTestModel>();
    serverProperty.slave();
    auto clientProperty = RdProperty<InterningTestModel>();

    serverProperty.identify(*serverIdentities, RdId(1L));
    clientProperty.identify(*clientIdentities, RdId(1L));

    bindStatic(serverProtocol.get(), serverProperty, "top");
    bindStatic(clientProtocol.get(), clientProperty, "top");

    /*val serverModel = InterningTestModel("")

    val simpleTestData = simpleTestData

    simpleTestData.forEach { (k, v) ->
                serverModel.issues[k] = WrappedStringModel(v)
    }

    serverProperty.set(serverModel)

    val clientModel = clientProperty.valueOrThrow

    simpleTestData.forEach { (k, v) ->
                assertEquals(v, clientModel.issues[k]!!.text)
    }*/

}