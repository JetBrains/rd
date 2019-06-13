#include "CrossTestClientBase.h"

#include "experimental/filesystem"

CrossTestClientBase::CrossTestClientBase() : CrossTestBase() {
	uint16_t port = 0;
	std::ifstream input(tmp_directory);
	for (int i = 0; i < 5 && !input.good(); std::this_thread::sleep_for(std::chrono::seconds(1)), ++i) {
		input = std::ifstream(tmp_directory);
	}
	assert(!input.good());
	input >> port;

	wire = std::make_shared<rd::SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	protocol = std::make_unique<rd::Protocol>(rd::Identities::CLIENT, &scheduler, wire, lifetime);
}
