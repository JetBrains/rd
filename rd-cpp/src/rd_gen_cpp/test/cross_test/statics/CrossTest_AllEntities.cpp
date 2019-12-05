#include "CrossTest_AllEntities.h"

#include <DemoModel/Derived.h>

namespace rd {
	namespace cross {
		void CrossTestAllEntities::fireAll(const demo::DemoModel &model, const demo::ExtModel &extModel) {
			model.get_boolean_property().set(false);

			model.get_boolean_array().emplace(std::vector<bool>{false, false, true});

			auto scalar = demo::MyScalar(false,
										 98,
										 32000,
										 1'000'000'000,
										 -2'000'000'000'000'000'000,
										 3.14f,
										 -123456789.012345678,
										 std::numeric_limits<uint8_t>::max() - 1,
										 std::numeric_limits<uint16_t>::max() - 1,
										 std::numeric_limits<uint32_t>::max() - 1,
										 std::numeric_limits<uint64_t>::max() - 1,
										 demo::MyEnum::cpp,
										 demo::Flags::anyFlag | demo::Flags::cppFlag,
										 demo::MyInitializedEnum::hundred
			);
#ifdef __cpp_structured_bindings
			auto[_bool, _byte, _short, _int, _long, _float, _double, _unsigned_byte, _unsigned_short, _unsigned_int, _unsigned_long, _enum, _flags] = scalar;
			auto const &[__bool, __byte, __short, __int, __long, __float, __double, __unsigned__byte, __unsigned__short, __unsigned__int, __unsigned__long, __enum, __flags] = scalar;
			auto[first, second] = ComplicatedPair(Derived(L"first"), Derived(L"second"));
#endif

			model.get_scalar().set(scalar);

			model.get_ubyte().set(98);

			model.get_ubyte_array().emplace(std::vector<uint8_t>{98, static_cast<uint8_t>(-1)});
			// model.get_list().add(9);
			// model.get_list().add(8);

			model.get_set().add(98);

			model.get_mapLongToString().set(98, L"Cpp");

			auto valA = L"Cpp";
			auto valB = L"protocol";

			model.get_interned_string().set(valA);
			model.get_interned_string().set(valA);
			model.get_interned_string().set(valB);
			model.get_interned_string().set(valB);
			model.get_interned_string().set(valA);

			auto derived = demo::Derived(L"Cpp instance");
			model.get_polymorphic().set(derived);

			const auto date_time = DateTime(std::time_t(0 + 98));//"1970-01-01 03:01:38"
			model.get_date().set(date_time);

			model.get_enum().set(demo::MyEnum::cpp);

			extModel.get_checker().fire();
		}
	}
}