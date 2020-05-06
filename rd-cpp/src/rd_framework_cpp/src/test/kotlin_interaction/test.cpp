#include "impl/RdProperty.h"
#include "protocol/Protocol.h"
#include "scheduler/SimpleScheduler.h"
#include "std/filesystem.h"
#include "thirdparty.hpp"
#include "wire/SocketWire.h"

#include <fstream>

using namespace rd;

int main()
{
	auto tmp_directory = filesystem::get_temp_directory() + "/rd/port.txt";
	std::ifstream fin(tmp_directory);
	uint16_t port;
	fin >> port;
	LifetimeDefinition definition(Lifetime::Eternal());
	Lifetime lifetime = definition.lifetime;

	LifetimeDefinition socketDefinition(Lifetime::Eternal());
	Lifetime socketLifetime = socketDefinition.lifetime;

	SimpleScheduler testScheduler;
	auto server = new SocketWire::Client(socketLifetime, &testScheduler, port, "TestClient");
	std::shared_ptr<IWire> wire(server);
	Protocol clientProtocol{Identities::CLIENT, &testScheduler, std::move(wire), lifetime};

	RdProperty<optional<int32_t>> property_main{0};
	property_main.rdid = RdId(1);
	property_main.bind(lifetime, &clientProtocol, "top");

	RdProperty<optional<int32_t>> property_rx{0};
	property_rx.rdid = RdId(2);
	property_rx.bind(lifetime, &clientProtocol, "rx");

	property_rx.advise(lifetime, [](optional<int32_t> const& x) { std::cout << "rx value changed to " << *x << "\n"; });
	for (int i = 1; i < 10; ++i)
	{
		property_main.set(i);
	}

	std::this_thread::sleep_for(std::chrono::minutes(5));

	socketDefinition.terminate();
	definition.terminate();
}