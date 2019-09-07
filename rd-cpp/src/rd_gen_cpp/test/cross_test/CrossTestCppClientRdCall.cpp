#include "CrossTestClientBase.h"

#include "DemoModel.h"

#include "to_string.h"

using namespace demo;

namespace rd {
	namespace cross {
		class CrossTestCppClientRdCall : public CrossTestClientBase {
			int run() override {
				DemoModel model;

				RdTask<int32_t> task;
				scheduler.queue([&]() mutable {
					model.connect(model_lifetime, protocol.get());

					model.get_call().set([](wchar_t c) -> std::wstring {
						return std::to_wstring(c);
					});

					task = model.get_callback().start(L"Cpp");
					task.advise(model_lifetime, [&](RdTaskResult<int32_t> const &result) {
						printAnyway(printer, rd::to_string(result));

						promise.set_value();
					});
				});

				terminate();
				return 0;
			}
		};
	}
}

int main(int argc, char **argv) {
	rd::cross::CrossTestCppClientRdCall test;
	return test.main(argc, argv, "CrossTestCppClientRdCall");
}