//
// Created by jetbrains on 3/1/2019.
//

#include "InterningTestBase.h"

#include "RdProperty.h"
#include "InterningProtocolLevelModel.h"

#include <numeric>

namespace rd {
	namespace test {
		using namespace util;

		int64_t InterningTestBase::measureBytes(IProtocol *protocol, std::function<void()> action) {
			auto const &wire = dynamic_cast<SimpleWire const &>(*protocol->wire);
			auto pre = wire.bytesWritten;
			action();
			return wire.bytesWritten - pre;
		}

		void InterningTestBase::doTest(bool firstClient, bool secondClient, bool thenSwitchSides) {
			RdProperty<InterningProtocolLevelModel> server_property;
			RdProperty<InterningProtocolLevelModel> client_property;
			statics(server_property, 1).slave();
			statics(client_property, 1);

			bindStatic(serverProtocol.get(), server_property, "top");
			bindStatic(clientProtocol.get(), client_property, "top");

			InterningProtocolLevelModel serverModel(L"");

//            server_property.set(serverModel)
			auto const &clientModel = client_property.get();

			auto simpleTestData = this->simpleTestData;

			auto firstSenderProtocol = (firstClient) ? clientProtocol.get() : serverProtocol.get();
			auto const &firstSenderModel = (firstClient) ? clientModel : serverModel;

			auto firstBytesWritten = measureBytes(firstSenderProtocol, [&]() {
				for (auto &p : simpleTestData) {
					firstSenderModel.get_issues().set(p.first, ProtocolWrappedStringModel(p.second));
				}
			});

			auto secondSenderProtocol = (secondClient) ? clientProtocol.get() : serverProtocol.get();
			auto const &secondSenderModel = (secondClient) ? clientModel : serverModel;

			auto secondBytesWritten = measureBytes(firstSenderProtocol, [&]() {
				for (auto &p : simpleTestData) {
					secondSenderModel.get_issues().set(p.first + simpleTestData.size(), ProtocolWrappedStringModel(p.second));
				}
			});

			auto sum = std::accumulate(simpleTestData.begin(), simpleTestData.end(), (size_t) 0,
									   [&](int32_t acc, std::pair<int32_t, std::wstring> it) -> size_t {
										   return acc += it.second.length();
									   });

			EXPECT_TRUE(firstBytesWritten - sum >= secondBytesWritten);

			AfterTest();
		}
	}
}
