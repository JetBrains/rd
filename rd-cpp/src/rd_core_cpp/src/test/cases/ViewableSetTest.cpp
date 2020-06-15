#include <gtest/gtest.h>

#include "reactive/ViewableSet.h"

#include "test_util.h"

using namespace rd;

TEST(viewable_set, advise)
{
	std::unique_ptr<IViewableSet<int>> set = std::make_unique<ViewableSet<int>>();

	std::vector<int> logAdvise;
	std::vector<int> logView1;
	std::vector<int> logView2;
	LifetimeDefinition::use([&](Lifetime lt) {
		set->advise(lt, [&](AddRemove kind, int const& v) { logAdvise.push_back(kind == AddRemove::ADD ? v : -v); });
		set->view(lt, [&](Lifetime inner, int const& v) {
			logView1.push_back(v);
			inner->add_action([&]() { logView1.push_back(-v); });
		});
		set->view(Lifetime::Eternal(), [&](Lifetime inner, int const& v) {
			logView2.push_back(v);
			inner->add_action([&logView2, v]() { logView2.push_back(-v); });
		});

		EXPECT_TRUE(set->add(1));	 // 1
		// EXPECT_TRUE(set.add(arrayOf(1, 2)) } //1, 2
		EXPECT_FALSE(set->add(1));	  // 1
		EXPECT_TRUE(set->add(2));	  // 1, 2

		// EXPECT_TRUE(set.add(arrayOf(1, 2)) } //1, 2
		EXPECT_FALSE(set->add(1));	  // 1, 2
		EXPECT_FALSE(set->add(2));	  // 1, 2

		//        EXPECT_TRUE{set.removeAll(arrayOf(2, 3))} // 1
		EXPECT_TRUE(set->remove(2));	 // 1
		EXPECT_FALSE(set->remove(3));	 // 1

		EXPECT_TRUE(set->add(2));	  // 1, 2
		EXPECT_FALSE(set->add(2));	  // 1, 2

		//        EXPECT_TRUE(set.retainAll(arrayOf(2, 3)) // 2
		EXPECT_TRUE(set->remove(1));	 // 2
		EXPECT_FALSE(set->remove(3));	 // 2
	});

	EXPECT_TRUE(set->add(1));

	std::vector<int> expectedAdvise{1, 2, -2, 2, -1};
	EXPECT_EQ(expectedAdvise, logAdvise);

	std::vector<int> expectedView1{1, 2, -2, 2, -1, -2};
	EXPECT_EQ(expectedView1, logView1);

	std::vector<int> expectedView2{1, 2, -2, 2, -1, 1};
	EXPECT_EQ(expectedView2, logView2);
}

TEST(viewable_set, view)
{
	using listOf = std::vector<int>;

	listOf elementsView{2, 0, 1, 8, 3};
	listOf elementsUnView{1, 3, 8, 0, 2};

	size_t C = elementsView.size();

	std::unique_ptr<IViewableSet<int>> set = std::make_unique<ViewableSet<int>>();
	std::vector<std::string> log;
	auto x = LifetimeDefinition::use([&](Lifetime lifetime) {
		set->view(lifetime, [&](Lifetime lt, int const& value) {
			log.push_back("View " + std::to_string(value));
			lt->add_action([&]() { log.push_back("UnView " + std::to_string(value)); });
		});
		for (auto x : elementsView)
		{
			set->add(x);
		}
		return set->remove(1);
	});
	EXPECT_TRUE(x);
	EXPECT_EQ(C - 1, set->size());

	std::vector<std::string> expected(2 * C);
	for (size_t i = 0; i < C; ++i)
	{
		expected[i] = "View " + std::to_string(elementsView[i]);
		expected[C + i] = "UnView " + std::to_string(elementsUnView[i]);
	}
	EXPECT_EQ(expected, log);

	log.clear();
	LifetimeDefinition::use([&](Lifetime lifetime) {
		set->view(lifetime, [&](Lifetime lt, int const& value) {
			log.push_back("View " + std::to_string(value));
			lt->add_action([&]() { log.push_back("UnView " + std::to_string(value)); });
		});
		set->clear();
	});
	EXPECT_TRUE(set->empty());

	listOf rest_elements{2, 0, 8, 3};

	size_t K = C - 1;
	std::vector<std::string> expected2(2 * K);
	for (size_t i = 0; i < K; ++i)
	{
		expected2[i] = "View " + std::to_string(rest_elements[i]);
		expected2[K + i] = "UnView " + std::to_string(rest_elements[i]);
	}
	EXPECT_EQ(expected2, log);
}

TEST(viewable_set, add_remove_fuzz)
{
	std::unique_ptr<IViewableSet<int>> set = std::make_unique<ViewableSet<int>>();
	std::vector<std::string> log;

	const int C = 10;

	LifetimeDefinition::use([&](Lifetime lifetime) {
		set->view(lifetime, [&log](Lifetime lt, int const& value) {
			log.push_back("View " + std::to_string(value));
			lt->add_action([&]() { log.push_back("UnView " + std::to_string(value)); });
		});

		for (int i = 0; i < C; ++i)
		{
			set->add(i);
		}
	});

	for (int i = 0; i < C; ++i)
	{
		EXPECT_EQ("View " + std::to_string(i), log[i]);
		EXPECT_EQ("UnView " + std::to_string(C - i - 1), log[C + i]);
	}
}

TEST(viewable_set, move)
{
	ViewableSet<int> set1;
	ViewableSet<int> set2(std::move(set1));
}

using container = ViewableSet<int>;

static_assert(
	!std::is_constructible<container::iterator, std::nullptr_t>::value, "iterator should not be constructible from nullptr");

TEST(viewable_set_iterators, end_iterator)
{
	container c;
	container::iterator i = c.end();

	EXPECT_EQ(c.begin(), i);
}

TEST(viewable_set_iterators, reverse_iterators)
{
	container c;
	c.addAll({4, 3, 2, 1});

	EXPECT_EQ(1, *c.rbegin());
	EXPECT_EQ(2, *std::next(c.rbegin()));
	EXPECT_EQ(4, *std::prev(c.rend()));
}

TEST(viewable_set_iterators, iterators_postfix)
{
	container s;
	s.addAll({1, 2, 3});
	container::iterator i = s.begin();
	EXPECT_EQ(1, *i);
	container::iterator j = i++;
	EXPECT_EQ(2, *i);
	EXPECT_EQ(1, *j);
	j = i++;
	EXPECT_EQ(3, *i);
	EXPECT_EQ(2, *j);
	j = i++;
	EXPECT_EQ(s.end(), i);
	EXPECT_EQ(3, *j);
	j = i--;
	EXPECT_EQ(3, *i);
	EXPECT_EQ(s.end(), j);
}

std::vector<int> const perm4 = {2, 0, 1, 9};

TEST(viewable_set_iterators, fori)
{
	const container c;
	c.addAll(perm4);

	std::vector<int> log;
	for (auto const& item : c)
	{
		log.push_back(item);
	}

	EXPECT_EQ(log, perm4);
}

TEST(viewable_set_iterators, random_access)
{
	container c;
	c.addAll(perm4);

	EXPECT_EQ(*(c.begin() + 2), 1);
	EXPECT_EQ(*(c.rbegin() + 2), 0);
}

/*TEST(viewable_set_iterators, insert_return_value) {
	container c;
	c.addAll({1, 2, 3, 4});

	container::iterator i = c.add(std::next(c.begin(), 2), 5);
	EXPECT_EQ(5, *i);
	EXPECT_EQ(2, *std::prev(i));
	EXPECT_EQ(3, *std::next(i));
}

TEST(viewable_set_iterators, erase_return_value) {
	container c;
	c.addAll({1, 2, 3, 4});
	container::iterator i = c.remove(std::next(c.begin()));
	EXPECT_EQ(3, *i);
	i = c.remove(i);
	EXPECT_EQ(4, *i);
}*/
