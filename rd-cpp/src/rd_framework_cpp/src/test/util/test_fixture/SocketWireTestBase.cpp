#include "SocketWireTestBase.h"

#include "protocol/Identities.h"
#include "wire/SocketWire.h"

namespace rd
{
namespace test
{
namespace util
{
Protocol SocketWireTestBase::server(Lifetime lifetime, uint16_t port)
{
	std::shared_ptr<IWire> wire = std::make_shared<SocketWire::Server>(lifetime, &serverScheduler, port, "TestServer");
	Protocol res(Identities::SERVER, &serverScheduler, std::move(wire), lifetime);
	res.get_serialization_context();
	serverScheduler.pump_one_message();	   // binding InternRoot
	return res;
}

Protocol SocketWireTestBase::client(Lifetime lifetime, Protocol const& serverProtocol)
{
	auto const* server = dynamic_cast<SocketWire::Server const*>(serverProtocol.get_wire());
	std::shared_ptr<IWire> wire = std::make_shared<SocketWire::Client>(lifetime, &clientScheduler, server->port, "TestClient");
	Protocol res(Identities::CLIENT, &clientScheduler, std::move(wire), lifetime);
	res.get_serialization_context();
	clientScheduler.pump_one_message();	   // binding InternRoot
	return res;
}

Protocol SocketWireTestBase::client(Lifetime lifetime, uint16_t port)
{
	std::shared_ptr<IWire> wire = std::make_shared<SocketWire::Client>(lifetime, &clientScheduler, port, "TestClient");
	Protocol res(Identities::CLIENT, &clientScheduler, std::move(wire), lifetime);
	res.get_serialization_context();
	clientScheduler.pump_one_message();	   // binding InternRoot
	return res;
}

void SocketWireTestBase::init(Protocol const& serverProtocol, Protocol const& clientProtocol, const RdBindableBase* serverEntity,
	const RdBindableBase* clientEntity)
{
	if (serverEntity)
	{
		statics(*serverEntity, property_id);
		serverEntity->bind(lifetime, &serverProtocol, static_name);
	}

	if (clientEntity)
	{
		statics(*clientEntity, property_id);
		clientEntity->bind(lifetime, &clientProtocol, static_name);
	}
}

void SocketWireTestBase::terminate()
{
	checkSchedulersAreEmpty();

	socketLifetimeDef.terminate();
	lifetimeDef.terminate();
}

void SocketWireTestBase::checkSchedulersAreEmpty()
{
	rd::util::sleep_this_thread(200);
	EXPECT_TRUE(clientScheduler.messages.empty());
	EXPECT_TRUE(serverScheduler.messages.empty());
}

void SocketWireTestBase::pump_both()
{
	serverScheduler.pump_one_message();
	clientScheduler.pump_one_message();
}
}	 // namespace util
}	 // namespace test
}	 // namespace rd
