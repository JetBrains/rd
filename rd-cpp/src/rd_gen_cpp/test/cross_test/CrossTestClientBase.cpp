#include "CrossTestClientBase.h"

CrossTestClientBase::CrossTestClientBase() : CrossTestBase() {
	uint16_t port = 0;
	std::ifstream input(tmp_directory);
	input >> port;

	wire = std::make_shared<rd::SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	protocol = std::make_unique<rd::Protocol>(rd::Identities::CLIENT, &scheduler, wire, lifetime);
}
