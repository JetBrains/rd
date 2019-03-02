//
// Created by jetbrains on 3/1/2019.
//

#ifndef RD_CPP_INTERNINGTESTBASE_H
#define RD_CPP_INTERNINGTESTBASE_H


#include "RdFrameworkTestBase.h"

namespace rd {
    namespace test {
        class InterningTestBase : public RdFrameworkTestBase {

        protected:
            std::vector<std::pair<int32_t, std::wstring> > simpleTestData{
                    {0, L""},
                    {1, L"test"},
                    {2, L"why"}
            };

            int64_t measureBytes(IProtocol *protocol, std::function<void()> action) {
                auto const &wire = dynamic_cast<SimpleWire const &>(*protocol->wire);
                auto pre = wire.bytesWritten;
                action();
                return wire.bytesWritten - pre;
            }

            void doTest(bool firstClient, bool secondClient, bool thenSwitchSides = false);

        };
    }
}


#endif //RD_CPP_INTERNINGTESTBASE_H
