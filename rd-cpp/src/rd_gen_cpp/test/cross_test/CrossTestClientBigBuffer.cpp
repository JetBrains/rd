#include "CrossTestClientBase.h"

#include "RdProperty.h"
#include "CrossTestBase.h"

#include <string>

using namespace rd;

class CrossTestClientBigBuffer : CrossTestClientBase {

public:
	CrossTestClientBigBuffer() = default;

	int run() {
		scheduler.queue([&]() mutable {
			RdProperty<std::wstring> property;

			property.rdid = RdId(1);

			property.advise(lifetime, [](std::wstring const &s) {
				std::wcout << s;
			});
		});

		terminate();

		return 0;
	}
};

int main(int argc, char **argv) {
	CrossTestClientBigBuffer test;
	return test.run();
}