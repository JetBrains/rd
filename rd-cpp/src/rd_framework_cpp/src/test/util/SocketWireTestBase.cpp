//
// Created by jetbrains on 27.08.2018.
//

#include "SocketWireTestBase.h"
#include "SocketWire.h"

Protocol SocketWireTestBase::server(Lifetime lifetime, uint16_t port) {
	std::shared_ptr<IWire> wire = std::make_shared<SocketWire::Server>(std::move(lifetime), &serverScheduler, port, "TestServer");
    return Protocol(Identities(Identities::SERVER), &serverScheduler, std::move(wire));
}

Protocol SocketWireTestBase::client(Lifetime lifetime, Protocol const &serverProtocol) {
    auto const *server = dynamic_cast<SocketWire::Server const *>(serverProtocol.wire.get());
    std::shared_ptr<IWire> wire =
            std::make_shared<SocketWire::Client>(std::move(lifetime), &clientScheduler, server->port, "TestClient");
    return Protocol(Identities(Identities::CLIENT), &clientScheduler, std::move(wire));
}

Protocol SocketWireTestBase::client(Lifetime lifetime, uint16_t port) {
    std::shared_ptr<IWire> wire =
            std::make_shared<SocketWire::Client>(std::move(lifetime), &clientScheduler, port, "TestClient");
    return Protocol(Identities(Identities::CLIENT), &clientScheduler, std::move(wire));
}
