#include "CrossTestServerBase.h"

#include "wire/SocketWire.h"

#include <cstdint>
#include <memory>

namespace rd
{
namespace cross
{
CrossTestServerBase::CrossTestServerBase() : CrossTestBase()
{
	auto ptr = std::make_shared<SocketWire::Server>(socket_lifetime, &scheduler, 0, "TestServer");
	wire = ptr;
	protocol = std::make_unique<Protocol>(Identities::SERVER, &scheduler, wire, socket_lifetime);

	uint16_t port = ptr->port;
	std::ofstream file(port_file);
	file << port;
}
}	 // namespace cross
}	 // namespace rd