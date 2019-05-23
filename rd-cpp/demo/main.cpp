#define NOMINMAX

#include "DemoModel.h"
#include "ExtModel.h"
#include "Derived.h"

#include "Lifetime.h"
#include "SocketWire.h"
#include "Protocol.h"
#include "SimpleScheduler.h"
#include "filesystem.h"
#include <cstdint>
#include <fstream>
#include <string>

using namespace rd;
using namespace demo;

int main() {
	SimpleScheduler scheduler;

	LifetimeDefinition definition(false);
	Lifetime lifetime = definition.lifetime;

	LifetimeDefinition socket_definition(false);
	Lifetime socket_lifetime = definition.lifetime;

	auto tmp_directory = filesystem::get_temp_directory() + "/rd/port.txt";
	//region Client initialization
	uint16_t port = 0;
	std::ifstream inputFile(tmp_directory);
	inputFile >> port;

	auto wire = std::make_shared<SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	auto protocol = std::make_unique<Protocol>(Identities::CLIENT, &scheduler, wire, lifetime);
	//endregion

	//region Server initialization
	/*auto wire = std::make_shared<SocketWire::Server>(socket_lifetime, &wire_scheduler, 0, "TestServer");
	auto protocol = std::make_unique<Protocol>(Identities(Identities::SERVER), &protocol_scheduler, wire);

	uint16_t port = wire->port;
	std::ofstream outputFile(tmp_directory);
	outputFile << port;*/
	//endregion

	DemoModel model;
	model.connect(lifetime, protocol.get());

	const MyScalar scalar_example{
			false,
			97,
			32000,
			1'000'000'000,
			-2'000'000'000'000'000'000,
			3.14f,
			-123456789.012345678,
			std::numeric_limits<uint8_t>::max() - 1,
			std::numeric_limits<uint16_t>::max() - 1,
			std::numeric_limits<uint32_t>::max() - 1,
			std::numeric_limits<uint64_t>::max() - 1
	};

	//region advise or view

	model.get_boolean_property().advise(lifetime, [&](bool b) {
		return;
	});
	model.get_scalar().advise(lifetime, [&](MyScalar const &scalar) {
		return;
	});

	model.get_list().advise(lifetime,
							[](RdList<int32_t, Polymorphic<int32_t>>::Event e) {//Event must be passed by value!!!
							});

	model.get_set().advise(lifetime, [](AddRemove addRemove, const int &x) {//AddRemove also must be passed by value
	});

	model.get_mapLongToString().advise_add_remove(lifetime, [](AddRemove addRemove, int64_t const &key,
															   std::wstring const &value) {
	});


	model.get_callback().set([](std::wstring const &s) -> int32_t {
		return static_cast<int32_t>(s.length());
	});

	model.get_interned_string().advise(lifetime, [](std::wstring const &s) {
		std::wcout << L"INTERNED " << s << std::endl;
	});

	model.get_polymorphic().advise(lifetime, [](Base const &base) {
		if (base.type_name() == "Derived") {
			auto const &d = dynamic_cast<Derived const &>(base);
			std::wcout << L"POLYMORPHIC " << d.get_string() << std::endl;
		}
	});
	//endregion



	//region ext

	ExtModel const &ext = ExtModel::getOrCreateExtensionOf(model);

	ext.get_checker().advise(lifetime, [](Void) {
		std::cout << "CHECK" << std::endl;
	});

	//endregion



	//region changes in extension

	ext.get_checker().fire();
	//endregion



	//region changes in root

	model.get_scalar().set(scalar_example);

	auto task = model.get_call().start(L'A');
	task.advise(lifetime, [](decltype(task)::result_type const &result) {
	});

	//auto sync_result = model.get_call().sync(L'B');

	//endregion

	//region interning

	std::wstring value_a = L"abacaba";
	std::wstring value_b = L"protocol";

	model.get_interned_string().set(value_a);
	model.get_interned_string().set(value_b);
	model.get_interned_string().set(value_a);
	model.get_interned_string().set(value_b);
	model.get_interned_string().set(value_a);
	//endregion

	//model.get_polymorphic().set(Derived(L"Cpp instance"));

	std::this_thread::sleep_for(std::chrono::minutes(100));
	return 0;
}