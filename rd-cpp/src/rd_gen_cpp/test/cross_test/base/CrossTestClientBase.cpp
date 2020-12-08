#include "CrossTestClientBase.h"

namespace rd
{
namespace cross
{
CrossTestClientBase::CrossTestClientBase() : CrossTestBase()
{
	uint16_t port = 0;
	std::ifstream input_label(port_file_closed);
	for (int i = 0; i < 50 && !input_label.good(); std::this_thread::sleep_for(std::chrono::milliseconds(100)), ++i)
	{
	}
	std::ifstream input = std::ifstream(port_file);
	RD_ASSERT_MSG(input.good(), "File with port is missing by path:" + port_file);
	RD_ASSERT_MSG(input >> port, "Is file empty?:" + port_file);
	std::cerr << "Port is " + std::to_string(port) << std::endl;

	wire = std::make_shared<rd::SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	protocol = std::make_unique<rd::Protocol>(rd::Identities::CLIENT, &scheduler, wire, model_lifetime);
}
}	 // namespace cross
}	 // namespace rd