//
// Created by jetbrains on 24.07.2018.
//

#include "RdFrameworkTestBase.h"

RdFrameworkTestBase::RdFrameworkTestBase() : clientLifetimeDef(Lifetime::Eternal()),
                                             serverLifetimeDef(Lifetime::Eternal()),
                                             clientLifetime(clientLifetimeDef.lifetime),
                                             serverLifetime(serverLifetimeDef.lifetime) {

	clientWire = std::make_shared<TestWire>(&clientScheduler);
	serverWire = std::make_shared<TestWire>(&serverScheduler);

    clientProtocol = std::unique_ptr<IProtocol>(
            std::make_unique<Protocol>(/*serializers, */clientIdentities, &clientScheduler,
                                                        clientWire));
    serverProtocol = std::unique_ptr<IProtocol>(
            std::make_unique<Protocol>(/*serializers,*/ serverIdentities, &serverScheduler,
                                                        serverWire));

    TestWire const *w1 = clientWire.get();
    TestWire const *w2 = serverWire.get();
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
