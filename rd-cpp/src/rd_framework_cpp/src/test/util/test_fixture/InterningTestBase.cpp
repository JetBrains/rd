#include "InterningTestBase.h"

#include "InterningRoot1/InterningProtocolLevelModel.Generated.h"
#include "InterningRoot1/InterningTestModel.Generated.h"

#include <numeric>

namespace rd
{
namespace test
{
using namespace util;

int64_t InterningTestBase::measureBytes(IProtocol* protocol, std::function<void()> action)
{
	auto wire = dynamic_cast<SimpleWire const*>(protocol->get_wire());
	auto pre = wire->bytesWritten;
	action();
	return wire->bytesWritten - pre;
}

void InterningTestBase::for_each(std::function<void(int32_t, std::wstring)> f)
{
	std::for_each(simpleTestData.begin(), simpleTestData.end(), [&](auto const& p) {
		auto const& k = p.first;
		auto const& v = p.second;
		f(k, v);
	});
}

void InterningTestBase::testIntern(bool firstClient, bool secondClient, bool thenSwitchSides)
{
	doTest<InterningTestModel, WrappedStringModel>(firstClient, secondClient, thenSwitchSides);
}

void InterningTestBase::testProtocolLevelIntern(bool firstClient, bool secondClient, bool thenSwitchSides)
{
	doTest<InterningProtocolLevelModel, ProtocolWrappedStringModel>(firstClient, secondClient, thenSwitchSides);
}
}	 // namespace test
}	 // namespace rd
