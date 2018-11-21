//
// Created by jetbrains on 27.08.2018.
//

#include "SocketWireTestBase.h"
#include "SocketWire.h"

Protocol SocketWireTestBase::server(Lifetime lifetime, uint16_t port) {
    SocketWire::Server *server = new SocketWire::Server(std::move(lifetime), &serverScheduler, port, "TestServer");
    std::shared_ptr<IWire> wire(server);
	return Protocol( Identities(IdKind::Server), &serverScheduler, std::move(wire) );
}

Protocol SocketWireTestBase::client(Lifetime lifetime, Protocol const &serverProtocol) {
    auto const *server = dynamic_cast<SocketWire::Server const *>(serverProtocol.wire.get());
    SocketWire::Client *client = new SocketWire::Client(std::move(lifetime), &clientScheduler, server->port,
                                                        "TestClient");
    std::shared_ptr<IWire> wire(client);
	return Protocol(Identities(), &clientScheduler, std::move(wire) );
}

Protocol SocketWireTestBase::client(Lifetime lifetime, uint16_t port) {
	SocketWire::Client *client = new SocketWire::Client(std::move(lifetime), &clientScheduler, port, "TestClient");
	std::shared_ptr<IWire> wire(client);
	return Protocol(Identities(), &clientScheduler, std::move(wire));
}
