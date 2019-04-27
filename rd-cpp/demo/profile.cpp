#include "DemoModel.h"
#include "ExtModel.h"

#include "Lifetime.h"
#include "SocketWire.h"
#include "Protocol.h"
#include "SimpleScheduler.h"

#include <cstdint>

using namespace rd;
using namespace demo;

int main() {
	minimum_level_to_log = LogLevel::Fatal;

	SimpleScheduler serverScheduler;
	SimpleScheduler clientScheduler;

	LifetimeDefinition server_definition(false);
	Lifetime server_lifetime = server_definition.lifetime;

	LifetimeDefinition server_socket_definition(false);
	Lifetime server_socket_lifetime = server_definition.lifetime;

	LifetimeDefinition client_definition(false);
	Lifetime client_lifetime = client_definition.lifetime;

	LifetimeDefinition client_socket_definition(false);
	Lifetime client_socket_lifetime = client_definition.lifetime;

	//region Client initialization
	uint16_t port = 0;

	auto serverWire = std::make_shared<SocketWire::Server>(server_socket_lifetime, &serverScheduler, port);
	auto serverProtocol = std::make_unique<Protocol>(Identities::SERVER, &serverScheduler, serverWire, server_lifetime);

	port = serverWire->port;

//	auto clientWire = std::make_shared<SocketWire::Client>(client_socket_lifetime, &clientScheduler, port);
//	auto clientProtocol = std::make_unique<Protocol>(Identities::CLIENT, &clientScheduler, clientWire, client_lifetime);

	DemoModel serverModel;
//	DemoModel clientModel;

	serverModel.connect(server_lifetime, serverProtocol.get());
//	clientModel.connect(client_lifetime, clientProtocol.get());	

	auto property = [&]() {
		const int C = 1000;
		for (int i = 0; i < C; ++i) {
			serverModel.get_boolean_property().set(i % 2 == 0);
		}
	};

	auto intern = [&]() {
		const int C = 1000;
		for (int i = 0; i < C; ++i) {
			serverModel.get_interned_string().set(L"abacaba" + std::to_wstring(i % 2));
		}
	};

	intern();

	std::this_thread::sleep_for(std::chrono::seconds(2));

	server_definition.terminate();
	client_definition.terminate();
}
