#include "DemoModel.h"
#include "ExtModel.h"

#include "Lifetime.h"
#include "SocketWire.h"
#include "Protocol.h"
#include "SimpleScheduler.h"

#include <cstdint>
#include <fstream>

using namespace rd;
using namespace rd::test;

int main() {
	SimpleScheduler scheduler;

	LifetimeDefinition definition(false);
	Lifetime lifetime = definition.lifetime;

	LifetimeDefinition socket_definition(false);
	Lifetime socket_lifetime = definition.lifetime;

	//region Client initialization
	uint16_t port = 0;
	std::ifstream inputFile("C:\\temp\\port.txt");
	inputFile >> port;

	auto wire = std::make_shared<SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	auto protocol = std::make_unique<Protocol>(Identities(Identities::CLIENT), &scheduler, wire);
	//endregion

	//region Server initialization
	/*auto wire = std::make_shared<SocketWire::Server>(socket_lifetime, &wire_scheduler, 0, "TestServer");
	auto protocol = std::make_unique<Protocol>(Identities(Identities::SERVER), &protocol_scheduler, wire);

	uint16_t port = wire->port;
	std::ofstream outputFile("C:\\temp\\port.txt");
	outputFile << port;*/
	//endregion

	DemoModel model;
	model.connect(lifetime, protocol.get());

	const MyScalar scalar_example{
			false,
			97,
			32000,
			1'000'000'000,
			-2'000'000'000'000'000'000
	};

	//region advise or view

	model.get_bool().advise(lifetime, [&](bool b) {
		return;
	});
	model.get_scalar().advise(lifetime, [&](MyScalar const &scalar) {
		return;
	});

	model.get_list().advise(lifetime, [](RdList<int32_t, Polymorphic < int32_t>>
	::Event
	e) {//Event must be passed by value!!!
	});

	model.get_set().advise(lifetime, [](AddRemove addRemove, const int &x) {//AddRemove also must be passed by value
	});

	model.get_mapLongToString().advise_add_remove(lifetime, [](AddRemove addRemove, int64_t const &key,
															   std::wstring const &value) {
	});


	model.get_callback().set([](std::wstring const &s) -> int32_t {
		return static_cast<int32_t>(s.length());
	});
	//endregion



	//region ext

	ExtModel const &ext = ExtModel::getOrCreateExtensionOf(model);

	ext.get_checker().advise(lifetime, [](void *) {
		std::cout << "CHECK" << std::endl;
	});
	//endregion



	//region changes in extension

	ext.get_checker().fire(nullptr);
	//endregion



	//region changes in root

	/*model.get_scalar().set(scalar_example);

	auto task = model.get_call().start(L'A');
	task.advise(lifetime, [](decltype(task)::result_type const &result) {
	});

	auto sync_result = model.get_call().sync(L'B');*/
	//endregion

	std::this_thread::sleep_for(std::chrono::minutes(100));
	return 0;
}