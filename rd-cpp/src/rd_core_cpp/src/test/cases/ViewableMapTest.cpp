#include <gtest/gtest.h>

#include "reactive/ViewableMap.h"

using namespace rd;
using namespace rd::util;

TEST(viewable_map, advise)
{
	std::unique_ptr<IViewableMap<int, int>> map = std::make_unique<ViewableMap<int, int>>();
	map->set(0, 1);
	map->set(1, 1);
	map->set(2, 2);
	map->set(0, 0);
	map->remove(2);

	std::vector<std::string> log_add_remove;
	std::vector<std::string> log_update;
	std::vector<int> log_view;

	LifetimeDefinition::use([&](Lifetime lifetime) {
		map->advise_add_remove(lifetime, [&](AddRemove kind, int const& key, int const& value) {
			log_add_remove.push_back(to_string(kind) + " " + std::to_string(key) + ":" + std::to_string(value));
		});
		map->advise(lifetime, [&](typename IViewableMap<int, int>::Event entry) { log_update.push_back(to_string(entry)); });
		map->view(lifetime, [&](Lifetime inner, const std::pair<int const*, int const*> x) {
			inner->bracket(
				[&log_view, x]() { log_view.push_back(*x.first); }, [&log_view, x]() { log_view.push_back(-*x.second); });
		});

		lifetime->add_action([&log_add_remove]() { log_add_remove.emplace_back("End"); });

		map->set(0, 1);
		map->set(10, 10);
		map->set(0, 0);
		map->remove(1);

		EXPECT_EQ(0, *map->get(0));
	});
	EXPECT_EQ(arrayListOf("Add 0:0"s, "Add 1:1"s, "Remove 0:0"s, "Add 0:1"s, "Add 10:10"s, "Remove 0:1"s, "Add 0:0"s, "Remove 1:1"s,
				  "End"s),
		log_add_remove);
	EXPECT_EQ(arrayListOf("Add 0:0"s, "Add 1:1"s, "Update 0:1"s, "Add 10:10"s, "Update 0:0"s, "Remove 1"s), log_update);
	EXPECT_EQ(arrayListOf(0, 1, -0, 0, 10, -1, 0, -1, /*this events are arguable*/ 0, -10), log_view);

	log_add_remove.clear();
	LifetimeDefinition::use([&](Lifetime lifetime) {
		map->advise_add_remove(lifetime, [&log_add_remove](AddRemove kind, int const& key, int const& value) {
			log_add_remove.push_back(to_string(kind) + " " + std::to_string(key) + ":" + std::to_string(value));
		});
		map->set(0, 0);

		EXPECT_FALSE(map->empty());
		map->clear();
		EXPECT_TRUE(map->empty());
	});

	EXPECT_EQ(arrayListOf("Add 0:0"s, "Add 10:10"s, "Remove 0:0"s, "Remove 10:10"s), log_add_remove);
}

TEST(viewable_map, view)
{
	using listOf = std::vector<int>;

	listOf elementsView{2, 0, 1, 8, 3};
	listOf elementsUnView{1, 3, 8, 0, 2};

	listOf indexesUnView{2, 4, 3, 1, 0};

	size_t C{elementsView.size()};

	std::unique_ptr<IViewableMap<int32_t, int32_t>> map = std::make_unique<ViewableMap<int32_t, int32_t>>();
	std::vector<std::string> log;
	LifetimeDefinition::use([&](Lifetime lifetime) {
		map->view(lifetime, [&](Lifetime lt, std::pair<int32_t const*, int32_t const*> value) {
			log.push_back("View " + to_string(value));
			lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
		});
		for (int32_t i = 0, size = static_cast<int32_t>(elementsView.size()); i < size; ++i)
		{
			map->set(i, elementsView[i]);
		}
		map->remove(2);
	});

	EXPECT_EQ(C - 1, map->size());

	std::vector<std::string> expected(2 * C);
	for (size_t i = 0; i < C; ++i)
	{
		expected[i] = "View (" + std::to_string(i) + ", " + std::to_string(elementsView[i]) + ")";
		expected[C + i] = "UnView (" + std::to_string(indexesUnView[i]) + ", " + std::to_string(elementsUnView[i]) + ")";
	}
	EXPECT_EQ(expected, log);
}

TEST(viewable_map, add_remove_fuzz)
{
	std::unique_ptr<IViewableMap<int32_t, int32_t>> map(new ViewableMap<int32_t, int32_t>());
	std::vector<std::string> log;

	const int C = 10;

	LifetimeDefinition::use([&](Lifetime lifetime) {
		map->view(lifetime, [&log](Lifetime lt, std::pair<int32_t const*, int32_t const*> value) {
			log.push_back("View " + to_string(value));
			lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
		});

		for (int i = 0; i < C; ++i)
		{
			map->set(i, 0);
		}
	});

	for (int i = 0; i < C; ++i)
	{
		EXPECT_EQ("View (" + std::to_string(i) + ", 0)", log[i]);
		EXPECT_EQ("UnView (" + std::to_string(C - i - 1) + ", 0)", log[C + i]);
	}
}

TEST(viewable_map, move)
{
	ViewableMap<int, int> set1;
	ViewableMap<int, int> set2(std::move(set1));
}

using container = ViewableMap<std::wstring, int>;

static_assert(
	!std::is_constructible<container::iterator, std::nullptr_t>::value, "iterator should not be constructible from nullptr");

TEST(viewable_map_iterators, end_iterator)
{
	container c;
	container::iterator i = c.end();

	EXPECT_EQ(c.begin(), i);
}

const std::vector<int> perm4 = {2, 0, 1, 9};
auto mapper = [](auto x) { return std::to_wstring(x); };

TEST(viewable_map_iterators, reverse_iterators)
{
	container c;
	for (int i : perm4)
	{
		c.set(mapper(i), i);
	}

	//	EXPECT_EQ(9, c.rbegin().value());
	EXPECT_EQ(9, *c.rbegin());
	EXPECT_EQ(1, *std::next(c.rbegin()));
	EXPECT_EQ(2, *std::prev(c.rend()));
}

const int perm3[] = {1, 2, 8};

TEST(viewable_map_iterators, iterators_postfix)
{
	container c;
	for (int i : perm3)
	{
		c.set(mapper(i), i);
	}
	container::iterator i = c.begin();
	EXPECT_EQ(1, *i);
	container::iterator j = i++;
	EXPECT_EQ(2, *i);
	EXPECT_EQ(1, *j);
	j = i++;
	EXPECT_EQ(8, *i);
	EXPECT_EQ(2, *j);
	j = i++;
	EXPECT_EQ(c.end(), i);
	EXPECT_EQ(8, *j);
	j = i--;
	EXPECT_EQ(8, *i);
	EXPECT_EQ(c.end(), j);
}

TEST(viewable_map_iterators, fori)
{
	const container c;
	for (int i : perm4)
	{
		c.set(mapper(i), i);
	}

	{
		int i = 0;
		for (auto const& it : c)
		{
			EXPECT_EQ(it, perm4[i]);
			++i;
		}
	}

	for (auto it = c.begin(); it != c.end(); ++it)
	{
		auto i = it - c.begin();
		EXPECT_EQ(it.key(), mapper(perm4[i]));
		EXPECT_EQ(it.value(), perm4[i]);
	}

	for (auto it = c.rbegin(); it != c.rend(); ++it)
	{
		auto i = perm4.size() - (it - c.rbegin()) - 1;
		auto const& type = it.key();
		EXPECT_EQ(type, mapper(perm4[i]));
		int const& value = it.value();
		EXPECT_EQ(value, perm4[i]);
	}
}
/*TEST(viewable_map_iterators, insert_return_value) {
	container c;
	c.addAll({1, 2, 3, 4});

	container::iterator i = c.add(std::next(c.begin(), 2), 5);
	EXPECT_EQ(5, *i);
	EXPECT_EQ(2, *std::prev(i));
	EXPECT_EQ(3, *std::next(i));
}

TEST(viewable_map_iterators, erase_return_value) {
	container c;
	c.addAll({1, 2, 3, 4});
	container::iterator i = c.remove(std::next(c.begin()));
	EXPECT_EQ(3, *i);
	i = c.remove(i);
	EXPECT_EQ(4, *i);
}*/
