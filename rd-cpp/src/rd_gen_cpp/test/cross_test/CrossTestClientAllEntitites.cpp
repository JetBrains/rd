#define NOMINMAX

#include "DemoModel.h"
#include "ExtModel.h"
#include "Derived.h"

#include "Lifetime.h"
#include "SocketWire.h"
#include "Protocol.h"
#include "SimpleScheduler.h"
#include "filesystem.h"
#include "CrossTestClientBase.h"

#include <cstdint>
#include <fstream>
#include <string>
#include <climits>
#include <future>

using namespace rd;
using namespace demo;

class CrossTestClientAllEntities : public CrossTestClientBase {
	void adviseAll(Lifetime lifetime, DemoModel const &model, ExtModel const &extModel, printer_t &printer,
				   std::promise<void> &promise);

	void fireAll(const DemoModel &model, const ExtModel &extModel);
public:
	int run() {
		DemoModel model;
		std::promise<void> promise;
		std::future<void> f = promise.get_future();

		scheduler.queue([&]() mutable {
			model.connect(lifetime, protocol.get());
			ExtModel const &extModel = ExtModel::getOrCreateExtensionOf(model);


			adviseAll(lifetime, model, extModel, printer, promise);
			fireAll(model, extModel);
//
		});

		auto status = f.wait_for(std::chrono::seconds(10));

		socket_definition.terminate();
		definition.terminate();

		for (const auto &item : printer) {
			std::cout << item << std::endl;
		}
		std::cout << std::endl;

		return 0;
	}
};

int main(int argc, char **argv) {
	CrossTestClientAllEntities test;
	return test.run();
}


void CrossTestClientAllEntities::adviseAll(Lifetime lifetime, DemoModel const &model, ExtModel const &extModel, printer_t &printer,
			   std::promise<void> &promise) {
	model.get_boolean_property().advise(lifetime, [&](bool const &it) {
		printIfRemoteChange(printer, model.get_boolean_property(), "boolean_property", it);
	});

	model.get_boolean_array().advise(lifetime, [&](std::vector<bool> const &it) {
		printIfRemoteChange(printer, model.get_boolean_array(), "boolean_array", it);
	});

	model.get_scalar().advise(lifetime, [&](MyScalar const &it) {
		printIfRemoteChange(printer, model.get_scalar(), "scalar", it);
	});

	model.get_ubyte().advise(lifetime, [&](uint8_t const &it) {
		printIfRemoteChange(printer, model.get_ubyte(), "ubyte", it);
	});

	model.get_ubyte_array().advise(lifetime, [&](std::vector<uint8_t> const &it) {
		printIfRemoteChange(printer, model.get_ubyte_array(), "ubyte_array", it);
	});

	model.get_list().advise(lifetime, [&](IViewableList<int32_t>::Event e) {
		//Event must be passed by value!!!
		printIfRemoteChange(printer, model.get_list(), "list", e);
	});

	model.get_set().advise(lifetime, [&](IViewableSet<int32_t>::Event e) {
		//Event must be passed by value!!!
		printIfRemoteChange(printer, model.get_set(), "set", e);
	});

	model.get_mapLongToString().advise(lifetime, [&](IViewableMap<int64_t, std::wstring>::Event e) {
		//Event must be passed by value!!!
		printIfRemoteChange(printer, model.get_mapLongToString(), "mapLongToString", e);
	});

	model.get_callback().set([&](std::wstring const &s) -> int32_t {
		printIfRemoteChange(printer, model.get_mapLongToString(), "callback:", s);
		return 14;
	});

	model.get_interned_string().advise(lifetime, [&](std::wstring const &it) {
		printIfRemoteChange(printer, model.get_interned_string(), "Interned", it);
	});

	model.get_polymorphic().advise(lifetime, [&](Base const &it) {
		printIfRemoteChange(printer, model.get_polymorphic(), "polymorphic", it);

	});

	model.get_enum().advise(lifetime, [&](MyEnum const& it){
		printIfRemoteChange(printer, model.get_enum(), "enum", it);
		if (!model.get_enum().is_local_change) {
			promise.set_value();
		}
	});

	extModel.get_checker().advise(lifetime, [&]() {
		printIfRemoteChange(printer, extModel.get_checker(), "extModel.checker", Void());
	});
}

void CrossTestClientAllEntities::fireAll(const DemoModel &model, const ExtModel &extModel) {
	model.get_boolean_property().set(false);

	model.get_boolean_array().emplace(std::vector<bool>{false, false, true});
	
	auto scalar = MyScalar(false,
						   98,
						   32000,
						   1'000'000'000,
						   -2'000'000'000'000'000'000,
						   3.14f,
						   -123456789.012345678,
						   std::numeric_limits<uint8_t >::max() - 1,
						   std::numeric_limits<uint16_t>::max() - 1,
						   std::numeric_limits<uint32_t>::max() - 1,
						   std::numeric_limits<uint64_t>::max() - 1,
						   MyEnum::cpp
	);
	model.get_scalar().set(scalar);

	model.get_ubyte().set(98);

	model.get_ubyte_array().emplace(std::vector<uint8_t>{98, static_cast<uint8_t>(-1)});
	// model.get_list().add(9);
	// model.get_list().add(8);

	model.get_set().add(98);

	model.get_mapLongToString().set(98, L"Cpp");

	auto valA = L"Cpp";
	auto valB = L"protocol";

	// auto res = model.get_call().sync(L'c');

	model.get_interned_string().set(valA);
	model.get_interned_string().set(valA);
	model.get_interned_string().set(valB);
	model.get_interned_string().set(valB);
	model.get_interned_string().set(valA);

	auto derived = Derived(L"Cpp instance");
	model.get_polymorphic().set(derived);

	model.get_enum().set(MyEnum::cpp);

	extModel.get_checker().fire();
}


static_assert(DemoModel::const_toplevel, "const_toplevel value is wrong");
static_assert(ConstU::const_enum == MyEnum::default_, "const _enum value is wrong");
//	static_assert(MyScalar::const_string == L"const_string_value", "const_string value is wrong");
//  std::char_traits::compare is not constexpr until C++17 at least in Clang
static_assert(Base::const_base == 'B', "const_base value is wrong");
