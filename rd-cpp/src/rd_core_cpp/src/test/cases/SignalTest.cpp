#include <gtest/gtest.h>

#include "test_util.h"

#include "reactive/interfaces.h"
#include "reactive/SignalX.h"

using namespace rd;

TEST(signal, advice)
{
	std::unique_ptr<ISignal<int>> s = std::make_unique<Signal<int>>();

	s->fire(-1);
	std::vector<int> log = LifetimeDefinition::use([&s](Lifetime lf) {
		int acc = 1;
		std::vector<int> result;
		s->advise(lf, [&result](int const& x) { result.push_back(x); });
		/*lf->add_action([&result]() {
			result.push_back(0);
		});*/
		// don't do it!
		s->fire(++acc);
		s->fire(++acc);
		return result;
	});

	std::vector<int> expected = {2, 3};
	EXPECT_EQ(expected, log);
}

TEST(signal, temporary_definition)
{
	std::unique_ptr<ISignal<int>> s = std::make_unique<Signal<int>>();
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

TEST(signal, bamboo)
{
	std::unique_ptr<ISignal<int>> s = std::make_unique<Signal<int>>();
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

TEST(signal, priority_advise)
{
	Signal<Void> signal;
	std::vector<int> log;
	signal.advise_eternal([&log] { log.push_back(1); });
	signal.advise_eternal([&log] { log.push_back(2); });

	priorityAdviseSection([&signal, &log]() {
		signal.advise_eternal([&log] { log.push_back(3); });
		signal.advise_eternal([&log] { log.push_back(4); });
	});

	signal.advise_eternal([&log] { log.push_back(5); });

	signal.fire();
	EXPECT_EQ((std::vector<int>{3, 4, 1, 2, 5}), log);
}

TEST(signal, testRecursion)
{
	Signal<Void> A;
	Signal<Void> B;
	LifetimeDefinition lifetimeA(Lifetime::Eternal());
	LifetimeDefinition lifetimeB(Lifetime::Eternal());
	std::vector<std::string> log;
	A.advise(lifetimeA.lifetime, [&] {
		log.emplace_back("A");
		lifetimeB.terminate();
	});
	A.advise(lifetimeB.lifetime, [&] { log.emplace_back("B"); });
	A.fire();
	EXPECT_EQ(std::vector<std::string>{"A"}, log);	  // do we expect {"A"} or {"A", "B"} ?
}

TEST(signal, move)
{
	Signal<int> signal1;
	Signal<int> signal2(std::move(signal1));
}

TEST(signal, void_specialization)
{
	Signal<Void> signal;
	LifetimeDefinition::use([&](Lifetime lf) {
		bool fired = false;
		signal.advise(lf, [&] { fired = true; });
		signal.fire();
		EXPECT_TRUE(fired);
	});
}