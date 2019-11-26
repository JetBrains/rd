#include "CrossTestClientBase.h"

#include "DemoModel.h"

#include "impl/RdProperty.h"

#include <string>

using namespace rd;
using namespace demo;

namespace rd {
	namespace cross {
		class CrossTest_BigBuffer_CppClient : public CrossTestClientBase {

		public:
			CrossTest_BigBuffer_CppClient() = default;

			int run() override {
				DemoModel model;

				scheduler.queue([&]() mutable {
					model.connect(model_lifetime, protocol.get());

					IProperty<std::wstring> const &entity = model.get_property_with_default();

					int count = 0;

					entity.emplace(std::wstring(100'000, '9'));
					entity.emplace(std::wstring(100'000, '8'));
				});

				terminate();

				return 0;
			}
		};
	}
}

int main(int argc, char **argv) {
	rd::cross::CrossTest_BigBuffer_CppClient test;
	return test.main(argc, argv, "CrossTest_BigBuffer_CppClient");
}