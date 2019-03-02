//
// Created by jetbrains on 3/1/2019.
//

#include <gtest/gtest.h>

#include "RdProperty.h"
#include "InterningTestBase.h"

#include <cstdint>

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

