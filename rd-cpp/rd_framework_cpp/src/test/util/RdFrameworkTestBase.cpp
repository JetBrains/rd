//
// Created by jetbrains on 24.07.2018.
//

#include "RdFrameworkTestBase.h"

RdFrameworkTestBase::RdFrameworkTestBase() : clientLifetimeDef(Lifetime::Eternal()),
                                             serverLifetimeDef(Lifetime::Eternal()),
                                             clientLifetime(clientLifetimeDef.lifetime),
                                             serverLifetime(serverLifetimeDef.lifetime) {

    clientProtocol = std::unique_ptr<IProtocol>(
            std::make_unique<Protocol>(/*serializers, */clientIdentities, &clientScheduler,
                                                        std::make_shared<TestWire>(&clientScheduler)));
    serverProtocol = std::unique_ptr<IProtocol>(
            std::make_unique<Protocol>(/*serializers,*/ serverIdentities, &serverScheduler,
                                                        std::make_shared<TestWire>(&serverScheduler)));

    clientWire = std::dynamic_pointer_cast<TestWire>(clientProtocol->wire);
    serverWire = std::dynamic_pointer_cast<TestWire>(serverProtocol->wire);

    std::pair<TestWire const *, TestWire const *> p = std::make_pair(
            dynamic_cast<TestWire const *>(clientProtocol->wire.get()),
            dynamic_cast<TestWire const *>(serverProtocol->wire.get()));
    TestWire const *w1 = p.first;
    TestWire const *w2 = p.second;
    w1->counterpart = w2;
    w2->counterpart = w1;
}

void RdFrameworkTestBase::AfterTest() {
    clientLifetimeDef.terminate();
    serverLifetimeDef.terminate();
}

void RdFrameworkTestBase::setWireAutoFlush(bool flag) {
    clientWire->set_auto_flush(flag);
    serverWire->set_auto_flush(flag);
}
