//
// Created by jetbrains on 3/1/2019.
//

#include <numeric>
#include "InterningTestBase.h"

#include "RdProperty.h"
#include "InterningProtocolLevelModel.h"

namespace rd {
    namespace test {
        using namespace util;

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
            auto const& firstSenderModel = (firstClient) ? clientModel : serverModel;

            auto firstBytesWritten = measureBytes(firstSenderProtocol, [&]() {
                for (auto &p : simpleTestData) {
                    firstSenderModel.get_issues().set(p.first, ProtocolWrappedStringModel(p.second));
                }
            });

            auto secondSenderProtocol = (secondClient) ? clientProtocol.get() : serverProtocol.get();
            auto const& secondSenderModel = (secondClient) ? clientModel : serverModel;

            auto secondBytesWritten = measureBytes(firstSenderProtocol, [&]() {
                for (auto &p : simpleTestData) {
                    secondSenderModel.get_issues().set(p.first + simpleTestData.size(), ProtocolWrappedStringModel(p.second));
                }
            });

            std::accumulate(simpleTestData.begin(), simpleTestData.end(), 0, [](int32_t acc, std::pair<int32_t, std::wstring> it){
                acc += it.second.length + extraString.length;
            });
            assertTrue(firstBytesWritten - simpleTestData.sumBy
            { it.second.length } >= secondBytesWritten)

            val
            firstReceiver =
            if (firstClient) serverModel else clientModel
            val
            secondReceiver =
            if (secondClient) serverModel else clientModel

            simpleTestData.forEach
            {
                (k, v)->
                        assertEquals(v, firstReceiver.issues[k]
                !!.text)
                assertEquals(v, secondReceiver.issues[k + simpleTestData.size]
                !!.text)
            }

            if (!thenSwitchSides)
                return

                        val
            extraString = "again"

            val thirdBytesWritten = measureBytes(secondSenderProtocol) {
                simpleTestData.forEach
                {
                    (k, v)->
                            secondSenderModel.issues[k + simpleTestData.size * 2] = ProtocolWrappedStringModel(v + extraString)
                }
            }


            val fourthBytesWritten = measureBytes(firstSenderProtocol) {
                simpleTestData.forEach
                {
                    (k, v)->
                            firstSenderModel.issues[k + simpleTestData.size * 3] = ProtocolWrappedStringModel(v + extraString)
                }
            }

            assertTrue(thirdBytesWritten - simpleTestData.sumBy
            { it.second.length + extraString.length } >= fourthBytesWritten)

            simpleTestData.forEach
            {
                (k, v)->
                        assertEquals(v + extraString, secondReceiver.issues[k + simpleTestData.size * 2]
                !!.text)
                assertEquals(v + extraString, firstReceiver.issues[k + simpleTestData.size * 3]
                !!.text)
            }
        }
    }
}
