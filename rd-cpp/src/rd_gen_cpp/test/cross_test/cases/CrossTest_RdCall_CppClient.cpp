#include "CrossTestClientBase.h"

#include "DemoModel.h"

#include "std/to_string.h"

using namespace demo;

namespace rd
{
namespace cross
{
class CrossTest_RdCall_CppClient : public CrossTestClientBase
{
	int run() override
	{
		DemoModel model;

		RdTask<int32_t> task;
		scheduler.queue([&]() mutable {
			model.connect(model_lifetime, protocol.get());

			model.get_call().set([](wchar_t c) -> std::wstring { return std::to_wstring(c); });

			task = model.get_callback().start(L"Cpp");
		});

		terminate();
		return 0;
	}
};
}	 // namespace cross
}	 // namespace rd

int main(int argc, char** argv)
{
	rd::cross::CrossTest_RdCall_CppClient test;
	return test.main(argc, argv, "CrossTest_RdCall_CppClient");
}