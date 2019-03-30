//
// Created by jetbrains on 24.07.2018.
//

#include "RdFrameworkTestBase.h"

namespace rd {
	namespace test {
		RdFrameworkTestBase::RdFrameworkTestBase() : clientLifetimeDef(Lifetime::Eternal()),
													 serverLifetimeDef(Lifetime::Eternal()),
													 clientLifetime(clientLifetimeDef.lifetime),
													 serverLifetime(serverLifetimeDef.lifetime) {

			clientWire = std::make_shared<SimpleWire>(&clientScheduler);
			serverWire = std::make_shared<SimpleWire>(&serverScheduler);

			clientProtocol = std::unique_ptr<IProtocol>(
					std::make_unique<Protocol>(/*serializers, */clientIdentities, &clientScheduler,
																clientWire, clientLifetime));
			serverProtocol = std::unique_ptr<IProtocol>(
					std::make_unique<Protocol>(/*serializers,*/ serverIdentities, &serverScheduler,
																serverWire, serverLifetime));

			SimpleWire const *w1 = clientWire.get();
			SimpleWire const *w2 = serverWire.get();
			w1->counterpart = w2;
			w2->counterpart = w1;
		}

		void RdFrameworkTestBase::AfterTest() {
			clientLifetimeDef.terminate();
			serverLifetimeDef.terminate();

			after_test_called = true;
		}

		void RdFrameworkTestBase::setWireAutoFlush(bool flag) {
			clientWire->set_auto_flush(flag);
			serverWire->set_auto_flush(flag);
		}

		RdFrameworkTestBase::~RdFrameworkTestBase() {
			if (!after_test_called) {
				Logger().warn("Call AfterTest method in test function body");
			}
		}
	}
}
