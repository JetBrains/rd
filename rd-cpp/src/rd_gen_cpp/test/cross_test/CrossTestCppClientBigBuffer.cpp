#include "CrossTestClientBase.h"

#include "DemoModel.h"

#include "RdProperty.h"

#include <string>

using namespace rd;
using namespace demo;

namespace rd {
	namespace cross {
		class CrossTestCppClientBigBuffer : public CrossTestClientBase {

		public:
			CrossTestCppClientBigBuffer() = default;

			int run() override {
				DemoModel model;

				scheduler.queue([&]() mutable {
					model.connect(model_lifetime, protocol.get());

					IProperty<std::wstring> const &entity = model.get_property_with_default();

					int count = 0;
					entity.advise(model_lifetime, [&](std::wstring const &it) {
						printIfRemoteChange(printer, entity, "property_with_default", it);
						if (!is_local_change_of(entity) && is_default_value_changed(entity)) {
							if (++count == 2) {
								promise.set_value();
							}
						}
					});

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
	rd::cross::CrossTestCppClientBigBuffer test;
	return test.main(argc, argv, "CrossTestCppClientBigBuffer");
}