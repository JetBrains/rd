//
// Created by jetbrains on 11.07.2018.
//

#include <gtest/gtest.h>

#include "Property.h"

TEST(property, advise) {
    int acc = 0;

    std::unique_ptr<IProperty<int> > property(new Property<int>(acc));

    property->set(++acc);

    std::vector<int> log;
    Lifetime::use<int>([&property, &acc, &log](Lifetime lifetime) {
        property->advise(lifetime, [&log](int const &x) {
            log.push_back(-x);
        });
        property->view(lifetime, [&log](Lifetime inner, int const &x) {
            inner->bracket(
                    [&log, &x]() { log.push_back(x); },
                    [&log, &x]() { log.push_back(10 + x); }
            );
        });

        lifetime->add_action([&log]() { log.push_back(0); });

        property->set(property->get());
        property->set(++acc);
        property->set(++acc);

        return 0;
    });

    EXPECT_EQ(log.size(), 10);

    property->set(++acc);

    std::vector<int> expected{-1, 1, 11, -2, 2, 12, -3, 3, 0, 13};
    EXPECT_EQ(expected, log);
}

TEST(property, when_true) {
    int acc1 = 0;
    int acc2 = 0;

    std::unique_ptr<IProperty<bool>> property(new Property<bool>(false));
    property->set(true);
    Lifetime::use<int>([&](Lifetime lifetime) {
        property->view(lifetime, [&acc1](Lifetime lt, bool const &flag) {
            if (flag) {
                acc1++;
            }
        });

        property->view(lifetime, [&](Lifetime lt, bool const &flag) {
            if (flag) {
                lt->bracket(
                        [&acc2]() { acc2 += 2; },
                        [&acc2]() { acc2 -= 1; }
                );
            }
        });

        EXPECT_EQ(1, acc1);
        EXPECT_EQ(2, acc2);

        property->set(true);
        EXPECT_EQ(1, acc1);
        EXPECT_EQ(2, acc2);

        property->set(false);
        EXPECT_EQ(1, acc1);
        EXPECT_EQ(1, acc2);

        property->set(true);
        EXPECT_EQ(2, acc1);
        EXPECT_EQ(3, acc2);

        return 0;
    });
    EXPECT_EQ(2, acc1);
    EXPECT_EQ(2, acc2);
}

TEST(property, view) {
    std::unique_ptr<IProperty<int>> property(new Property<int>(1));
    int save = 0;

    Lifetime::use<int>([&](Lifetime lifetime) {
        property->view(lifetime, [&](Lifetime lt, int const &value) {
            save = value;
        });
        property->set(2);
        EXPECT_EQ(2, save);
        lifetime->add_action([&property]() { property->set(-1); });
        EXPECT_EQ(2, save);

        return 0;
    });

    EXPECT_EQ(-1, property->get());
    EXPECT_EQ(-1, save);
}

TEST(property, uninitialized) {
    Property<int> property;

    EXPECT_THROW(property.get(), std::exception);

    std::vector<int> log;

    Lifetime::use([&](Lifetime lifetime) {
        property.advise(lifetime, [&log](int const &val) {
            log.push_back(val);
        });

        EXPECT_TRUE(log.empty());

        property.set(~0);

        EXPECT_EQ(~0, log[0]);
    });

    property.set(0);

    EXPECT_EQ(1, log.size());
}