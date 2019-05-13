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

using printer_t = std::vector<std::string>;

void adviseAll(Lifetime, DemoModel const &, ExtModel const &, printer_t &);

std::wstring fireAll(const DemoModel &model, const ExtModel &extModel);

template<typename T>
void print(printer_t &printer, T const &x) {
	printer.push_back(rd::to_string(x));
}

int main() {
	SimpleScheduler scheduler;

	LifetimeDefinition definition(false);
	Lifetime lifetime = definition.lifetime;

	LifetimeDefinition socket_definition(false);
	Lifetime socket_lifetime = definition.lifetime;

	auto tmp_directory = filesystem::get_temp_directory() + "/rd/port.txt";

	//region Client initialization
	uint16_t port = 0;
	std::ifstream input(tmp_directory);
	input >> port;

	std::shared_ptr<IWire> wire = std::make_shared<SocketWire::Client>(socket_lifetime, &scheduler, port, "TestClient");
	std::unique_ptr<IProtocol> protocol = std::make_unique<Protocol>(Identities::CLIENT, &scheduler, wire, lifetime);
	//endregion

	//region Server initialization
	/*auto wire = std::make_shared<SocketWire::Server>(socket_lifetime, &wire_scheduler, 0, "TestServer");
	std::unique_ptr<IProtocol> protocol = std::make_unique<Protocol>(Identities(Identities::SERVER), &protocol_scheduler, wire);

	uint16_t port = wire->port;
	std::ofstream outputFile(tmp_directory);
	outputFile << port;*/
	//endregion

	printer_t printer;
	DemoModel model;
	ExtModel const &extModel = ExtModel::getOrCreateExtensionOf(model);
	model.connect(lifetime, protocol.get());

	scheduler.queue([&] {
		adviseAll(lifetime, model, extModel, printer);
	});

	scheduler.queue([&] {
		auto res = fireAll(model, extModel);
		print(printer, res);
	});

	std::this_thread::sleep_for(std::chrono::seconds(10));

	for (const auto &item : printer) {
		std::cout << item << std::endl;
	}
	std::cout << std::endl;
	return 0;
}

void adviseAll(Lifetime lifetime, DemoModel const &model, ExtModel const &extModel, printer_t &printer) {
	model.get_boolean_property().advise(lifetime, [&](bool const &it) {
		print(printer, "BooleanProperty:");
		print(printer, it);
	});

	model.get_scalar().advise(lifetime, [&](MyScalar const &it) {
		print(printer, "Scalar:");
		print(printer, it);
	});

	model.get_list().advise(lifetime, [&](IViewableList<int32_t>::Event e) {//Event must be passed by value!!!
		print(printer, "RdList:");
		print(printer, e);
	});

	model.get_set().advise(lifetime, [&](IViewableSet<int32_t>::Event e) {//Event must be passed by value!!!
		print(printer, "RdSet:");
		print(printer, e);
	});

	model.get_mapLongToString().advise(lifetime,
									   [&](IViewableMap<int64_t, std::wstring>::Event e) {//Event must be passed by value!!!
										   print(printer, "RdMap:");
										   print(printer, e);
									   });

	model.get_callback().set([&](std::wstring const &s) -> int32_t {
		print(printer, "RdTask:");
		print(printer, s);

		return 14;
	});

	model.get_interned_string().advise(lifetime, [&](std::wstring const &it) {
		print(printer, "Interned:");
		print(printer, it);
	});

	model.get_polymorphic().advise(lifetime, [&](Base const &it) {
		print(printer, "Polymorphic:");
		print(printer, it);
	});

	extModel.get_checker().advise(lifetime, [&]() {
		print(printer, "ExtModel:Checker:");
		print(printer, rd::Void());
	});
}

std::wstring fireAll(const DemoModel &model, const ExtModel &extModel) {
	model.get_boolean_property().set(false);

	auto scalar = MyScalar(false,
						   98,
						   32000,
						   1'000'000'000,
						   -2'000'000'000'000'000'000,
						   3.14f,
						   -123456789.012345678
	);
	model.get_scalar().set(scalar);

	// model.get_list().add(9);
	// model.get_list().add(8);

	model.get_set().add(98);

	model.get_mapLongToString().set(98, L"Cpp");

	auto valA = L"Cpp";
	auto valB = L"protocol";

	// auto res = model.get_call().sync(L'c');
	auto res = L"";
	
	model.get_interned_string().set(valA);
	model.get_interned_string().set(valA);
	model.get_interned_string().set(valB);
	model.get_interned_string().set(valB);
	model.get_interned_string().set(valA);

	auto derived = Derived(L"Cpp instance");
	model.get_polymorphic().set(derived);

	extModel.get_checker().fire();

	return res;
}
