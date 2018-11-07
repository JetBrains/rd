//
// Created by jetbrains on 24.07.2018.
//

#ifndef RD_CPP_RDFRAMEWORKTESTBASE_H
#define RD_CPP_RDFRAMEWORKTESTBASE_H

#include <gtest/gtest.h>

#include <memory>

#include "TestWire.h"
#include "Identities.h"
#include "Protocol.h"
#include "TestScheduler.h"

inline TestScheduler testScheduler;

class RdFrameworkTestBase : public ::testing::Test {
public:
    Serializers serializers;

    LifetimeDefinition clientLifetimeDef;
    LifetimeDefinition serverLifetimeDef;

    Lifetime clientLifetime;
    Lifetime serverLifetime;

    std::unique_ptr<IProtocol> clientProtocol;
    std::unique_ptr<IProtocol> serverProtocol;

    TestScheduler clientScheduler;
    TestScheduler serverScheduler;

    std::shared_ptr<TestWire> clientWire;
    std::shared_ptr<TestWire> serverWire;

    //    /*std::unique_ptr<IWire>*/TestWire clientWire{&clientScheduler};
    //    /*std::unique_ptr<IWire>*/TestWire serverTestWire{&serverScheduler};

    std::shared_ptr<IIdentities> clientIdentities = std::make_shared<Identities>();
    std::shared_ptr<IIdentities> serverIdentities = std::make_shared<Identities>();

    //    private var disposeLoggerFactory: Closeable? = null

    //    @BeforeTest
    RdFrameworkTestBase();

    //    @AfterTest
    virtual void AfterTest();

    template<typename T>
    T &bindStatic(IProtocol *protocol, T &x, std::string const &name) const {
        Lifetime lf = (protocol == clientProtocol.get() ? clientLifetime : serverLifetime);
        x.bind(lf, protocol, name);
        return x;
    }

    template<typename T>
    T &bindStatic(IProtocol *protocol, T &x, int id) const {
        Lifetime lf = (protocol == clientProtocol.get() ? clientLifetime : serverLifetime);
        statics(x, id).bind(lf, protocol, "top");
        return x;
    }

    void setWireAutoFlush(bool flag);
};


#endif //RD_CPP_RDFRAMEWORKTESTBASE_H
