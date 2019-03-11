//
// Created by jetbrains on 13.11.2018.
//

#include "SocketWire.h"
#include "SimpleScheduler.h"
#include "RdProperty.h"

#include "optional.hpp"

#include <fstream>

using namespace rd;
using namespace rd::test;
using namespace rd::test::util;

int main() {
	std::ifstream fin("C:\\temp\\port.txt");
	uint16_t port;
	fin >> port;
	LifetimeDefinition definition(Lifetime::Eternal());
	Lifetime lifetime = definition.lifetime;

	LifetimeDefinition socketDefinition(Lifetime::Eternal());
	Lifetime socketLifetime = socketDefinition.lifetime;

	SimpleScheduler testScheduler;
	auto server = new SocketWire::Client(socketLifetime, &testScheduler, port, "TestClient");
	std::shared_ptr<IWire> wire(server);
	Protocol clientProtocol{Identities(Identities::CLIENT), &testScheduler, std::move(wire)};

	RdProperty<tl::optional<int32_t>> property_main{0};
	property_main.rdid = RdId(1);
	property_main.bind(lifetime, &clientProtocol, "top");

	RdProperty<tl::optional<int32_t>> property_rx{0};
	property_rx.rdid = RdId(2);
	property_rx.bind(lifetime, &clientProtocol, "rx");

	property_rx.advise(lifetime, [](tl::optional<int32_t> const &x) {
		std::cout << "rx value changed to " << *x << "\n";
	});
	for (int i = 1; i < 10; ++i) {
		property_main.set(i);
	}

	std::this_thread::sleep_for(std::chrono::minutes(5));

	socketDefinition.terminate();
	definition.terminate();
}