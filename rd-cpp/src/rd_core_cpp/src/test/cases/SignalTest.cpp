//
// Created by jetbrains on 09.07.2018.
//

#include <test_util.h>
#include "gtest/gtest.h"

#include "interfaces.h"
#include "SignalX.h"

using namespace rd;

TEST(signal, advice) {
    int acc = 0;
    std::unique_ptr<ISignal<int> > s(new Signal<int>());

    s->fire(++acc);

    std::vector<int> log;

    Lifetime::use<int>([&](Lifetime lf) {
                           s->advise(lf,
                                     [&log](int const &x) { log.push_back(x); }
                           );
                           lf->add_action([&log]() { log.push_back(0); });
                           s->fire(++acc);
                           s->fire(++acc);
                           return 0;
                       }
    );
    s->fire(++acc);

    std::vector<int> expected = {2, 3, 0};
    EXPECT_EQ(expected, log);
}

TEST(signal, temporary_definition) {
    std::unique_ptr<ISignal<int> > s(new Signal<int>());
    std::vector<int> log;

    LifetimeDefinition definition(Lifetime::Eternal());
    {
        LifetimeDefinition definition_son(definition.lifetime);
    }

    int acc = 0;
    s->advise(definition.lifetime, [&](int) { ++acc; });
    s->fire(0);
    EXPECT_EQ(1, acc);
    definition.terminate();
    s->fire(0);
    EXPECT_EQ(1, acc);
}

TEST(signal, bamboo) {
    std::unique_ptr<ISignal<int> > s(new Signal<int>());
    std::vector<int> log;

    LifetimeDefinition definition(Lifetime::Eternal());
    LifetimeDefinition definition_son(definition.lifetime);
    LifetimeDefinition definition_grand_son(definition_son.lifetime);

    int acc = 0;
    s->advise(definition_grand_son.lifetime, [&](int) { ++acc; });
    s->fire(0);
    EXPECT_EQ(1, acc);
    definition_son.terminate();
    s->fire(0);
    EXPECT_EQ(1, acc);
    s->fire(0);
    EXPECT_EQ(1, acc);

    definition.terminate();
    definition_son.terminate();
    definition_grand_son.terminate();
}

TEST(signal, priority_advise) {
    Signal<void *> signal;
    std::vector<int> log;
    signal.advise_eternal([&log](void *) { log.push_back(1); });
    signal.advise_eternal([&log](void *) { log.push_back(2); });

    priorityAdviseSection([&signal, &log]() {
        signal.advise_eternal([&log](void *) { log.push_back(3); });
        signal.advise_eternal([&log](void *) { log.push_back(4); });
    });

    signal.advise_eternal([&log](void *) { log.push_back(5); });


    signal.fire(nullptr);
    EXPECT_EQ((std::vector<int>{3, 4, 1, 2, 5}), log);
}

TEST(signal, testRecursion) {
    Signal<void *> A;
    Signal<void *> B;
    LifetimeDefinition lifetimeA(Lifetime::Eternal());
    LifetimeDefinition lifetimeB(Lifetime::Eternal());
    std::vector<std::string> log;
    A.advise(lifetimeA.lifetime, [&](void *) {
        log.emplace_back("A");
        lifetimeB.terminate();
    });
    A.advise(lifetimeB.lifetime, [&](void *) {
        log.emplace_back("B");
    });
    A.fire(nullptr);
    EXPECT_EQ(std::vector<std::string>{"A"}, log); //do we expect {"A"} or {"A", "B"} ?
}

TEST(signal, move) {
    Signal<int> signal1;
    Signal<int> signal2(std::move(signal1));
}