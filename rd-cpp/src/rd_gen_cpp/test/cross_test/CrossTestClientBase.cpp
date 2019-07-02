#include "CrossTestClientBase.h"

#include "experimental/filesystem"

CrossTestClientBase::CrossTestClientBase() : CrossTestBase() {
	uint16_t port = 0;
	std::ifstream input(tmp_directory);
	for (int i = 0; i < 5 && !input.good(); std::this_thread::sleep_for(std::chrono::seconds(1)), ++i) {
		input = std::ifstream(tmp_directory);
	}
	RD_ASSERT_MSG(input.good(), "File with port is missing by path:" + tmp_directory);
	RD_ASSERT_MSG(input >> port, "Is file empty?:" + tmp_directory);
	std::cerr << "Port is " + std::to_string(port) << std::endl;

	wire = std::make_shared<rd::SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	protocol = std::make_unique<rd::Protocol>(rd::Identities::CLIENT, &scheduler, wire, lifetime);
}
