//
// Created by jetbrains on 13.11.2018.
//

#include <fstream>

#include <impl/RdProperty.h>
#include "../../../../rd_core_cpp/src/main/lifetime/LifetimeDefinition.h"
#include "../../main/wire/SocketWire.h"
#include "../../main/base/IWire.h"
#include "../util/TestScheduler.h"
#include "../util/RdFrameworkTestBase.h"
#include "../../main/Identities.h"
#include "../../main/Protocol.h"

int main() {
    std::ifstream fin("C:\\temp\\port.txt");
    uint16_t port;
    fin >> port;
    LifetimeDefinition definition(Lifetime::Eternal());
    Lifetime lifetime = definition.lifetime;

    LifetimeDefinition socketDefinition(Lifetime::Eternal());
    Lifetime socketLifetime = socketDefinition.lifetime;

    TestScheduler testScheduler;
    auto server = new SocketWire::Client(socketLifetime, &testScheduler, port, "TestClient");
    std::shared_ptr<IWire> wire(server);
    auto clientProtocol = Protocol(Identities(IdKind::Server), &testScheduler, std::move(wire));

    RdProperty<int32_t> property{0};
    property.rdid = RdId(1);
    property.bind(lifetime, &clientProtocol, "top");

    for (int i = 1; i < 10; ++i) {
        property.set(i);
    }

    socketDefinition.terminate();
    definition.terminate();
}