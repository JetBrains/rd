//
// Created by jetbrains on 13.11.2018.
//

#include <fstream>
#include "../rd_core_cpp/src/main/lifetime/LifetimeDefinition.h"
#include "../rd_framework_cpp/src/main/Identities.h"
#include "../rd_framework_cpp/src/main/base/IWire.h"
#include "../rd_framework_cpp/src/main/wire/SocketWire.h"
#include "../rd_framework_cpp/src/test/util/RdFrameworkTestBase.h"
#include "../rd_framework_cpp/src/main/Protocol.h"

int main() {
    std::ifstream fin("C:\\temp\\port.txt");
    uint16_t port;
    fin >> port;
    LifetimeDefinition definition(Lifetime::Eternal());
    Lifetime lifetime = definition.lifetime;
    
    LifetimeDefinition socketDefinition(Lifetime::Eternal());
    Lifetime socketLifetime = socketDefinition.lifetime;
    
    SocketWire::Server *server = new SocketWire::Client(socketLifetime, &testScheduler, port, "TestClient");
    std::shared_ptr<IWire> wire(server);
    auto clientProtocol = Protocol(Identities(IdKind::Server), &testScheduler, std::move(wire));

    socketDefinition.terminate();
    definition.terminate();
}