#include <gtest/gtest.h>

#include "reactive/ViewableList.h"

using namespace rd;
using namespace rd::util;

TEST(viewable_list, add_remove_advise)
{
	std::unique_ptr<IViewableList<int>> list = std::make_unique<ViewableList<int>>();
	std::vector<std::string> log;
	LifetimeDefinition::use([&](Lifetime lifetime) {
		list->advise_add_remove(lifetime, [&log](AddRemove kind, size_t index, int const& value) {
			log.push_back(to_string(kind) + " " + std::to_string(index) + " " + std::to_string(value));
		});
		list->add(0);
		list->remove(0);
	});

	std::vector<std::string> expected{"Add 0 0"s, "Remove 0 0"s};
	EXPECT_EQ(expected, log);
}

TEST(viewable_list, add_remove_view)
{
	std::unique_ptr<IViewableList<int>> list = std::make_unique<ViewableList<int>>();
	std::vector<std::string> log;
	LifetimeDefinition::use([&](Lifetime lifetime) {
		list->view(lifetime, [&log](Lifetime lt, std::pair<size_t, int const*> value) {
			log.push_back("View " + to_string(value));
			lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
		});

		list->add(0);
		list->remove(0);
	});

	std::vector<std::string> expected{"View (0, 0)", "UnView (0, 0)"};
	EXPECT_EQ(expected, log);
}

TEST(viewable_list, add_remove_view2)
{
	std::unique_ptr<IViewableList<int>> list = std::make_unique<ViewableList<int>>();
	std::vector<std::string> log;
	LifetimeDefinition::use([&](Lifetime lifetime) {
		list->view(lifetime, [&log](Lifetime lt, std::pair<size_t, int const*> value) {
			log.push_back("View " + to_string(value));
			lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
		});

		list->add(0);
		list->set(0, 1);
		list->remove(1);
	});

	std::vector<std::string> expected{"View (0, 0)", "UnView (0, 0)", "View (0, 1)", "UnView (0, 1)"};
	EXPECT_EQ(expected, log);
}

TEST(viewable_list, add_remove_fuzz)
{
	std::unique_ptr<IViewableList<int>> list = std::make_unique<ViewableList<int>>();
	std::vector<std::string> log;

	const int C = 10;

	LifetimeDefinition::use([&](Lifetime lifetime) {
		list->view(lifetime, [&log](Lifetime lt, std::pair<size_t, int const*> value) {
			log.push_back("View " + to_string(value));
			lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
		});

		for (int i = 0; i < C; ++i)
		{
			list->add(0);
		}
	});

	for (int i = 0; i < C; ++i)
	{
		EXPECT_EQ("View (" + std::to_string(i) + ", 0)", log[i]);
		EXPECT_EQ("UnView (" + std::to_string(C - i - 1) + ", 0)", log[C + i]);
	}
}

TEST(viewable_list, insert_middle)
{
	std::unique_ptr<IViewableList<int>> list = std::make_unique<ViewableList<int>>();
	std::vector<std::string> log;
	LifetimeDefinition::use([&](Lifetime lifetime) {
		list->advise_add_remove(lifetime, [&log](AddRemove kind, size_t index, int const& value) {
			log.push_back(to_string(kind) + " " + std::to_string(index) + " " + std::to_string(value));
		});
		list->add(0);
		list->add(2);

		list->add(1, 1);
		std::vector<std::string> expected{"Add 0 0", "Add 1 2", "Add 1 1"};
		EXPECT_EQ(expected, log);

		return 0;
	});
}

TEST(viewable_list, other_reactive_api)
{
	std::unique_ptr<rd::IViewableList<int>> list = std::make_unique<ViewableList<int>>();
	std::vector<std::string> log;

	LifetimeDefinition::use([&](Lifetime lifetime) {
		list->advise_add_remove(lifetime, [&log](AddRemove kind, size_t index, int const& value) {
			log.push_back(to_string(kind) + " " + std::to_string(index) + " " + std::to_string(value));
		});
		list->add(0);
		list->add(0, 1);
		// EXPECT_EQ(log, arrayListOf({"Add 0 0"s, "Add 0 1"s}));
		EXPECT_EQ(log, (std::vector<std::string>{"Add 0 0", "Add 0 1"}));

		EXPECT_EQ(convert_to_list<int>(*list), arrayListOf(1, 0));
		log.clear();

		list->set(1, 2);
		EXPECT_EQ(log, arrayListOf("Remove 1 0"s, "Add 1 2"s));
		EXPECT_EQ(convert_to_list(*list), arrayListOf(1, 2));
		log.clear();

		list->clear();
		EXPECT_EQ(log, arrayListOf("Remove 1 2"s, "Remove 0 1"s));
		EXPECT_TRUE(list->empty());
		log.clear();

		list->add(1);
		list->addAll({1, 2});

		EXPECT_EQ(log, arrayListOf("Add 0 1"s, "Add 1 1"s, "Add 2 2"s));
		EXPECT_EQ(convert_to_list(*list), arrayListOf(1, 1, 2));
		log.clear();

		list->addAll(1, {3, 4});

		EXPECT_EQ(log, arrayListOf("Add 1 3"s, "Add 2 4"s));
		EXPECT_EQ(convert_to_list(*list), arrayListOf(1, 3, 4, 1, 2));
		log.clear();

		list->removeAll({1, 3});

		EXPECT_EQ(log, arrayListOf("Remove 3 1"s, "Remove 1 3"s, "Remove 0 1"s));
		EXPECT_EQ(convert_to_list(*list), arrayListOf(4, 2));
		log.clear();

		list->removeAt(0);
		EXPECT_EQ(log, arrayListOf("Remove 0 4"s));
		EXPECT_EQ(convert_to_list(*list), arrayListOf(2));
		log.clear();

		/*list->retainAll(listOf(1, 2))
		EXPECT_EQ(log, arrayListOf<String>(), "retainAll1")
				EXPECT_EQ(convert_to_list(*list), arrayListOf(2), "retainAll1")
		log.clear()

		list->retainAll(listOf(1))
		EXPECT_EQ(log, arrayListOf("Remove 0 2"), "retainAll2")
				EXPECT_EQ(convert_to_list(*list), arrayListOf<Int>(), "retainAll2")
		log.clear()

		assertTrue(list->add(0))
		assertTrue(list->add(0))*/
	});
}

TEST(viewable_list, move)
{
	ViewableList<int> list1;
	ViewableList<int> list2(std::move(list1));
}

using container = ViewableList<int>;

static_assert(
	!std::is_constructible<container::iterator, std::nullptr_t>::value, "iterator should not be constructible from nullptr");

TEST(viewable_list_iterators, end_iterator)
{
	container c;
	container::iterator i = c.end();

	EXPECT_EQ(c.begin(), i);
}

std::vector<int> const perm4 = {2, 0, 1, 9};

TEST(viewable_list_iterators, reverse_iterators)
{
	container c;
	c.addAll(perm4);

	EXPECT_EQ(9, *c.rbegin());
	EXPECT_EQ(1, *std::next(c.rbegin()));
	EXPECT_EQ(2, *std::prev(c.rend()));
}

TEST(viewable_list_iterators, iterators_postfix)
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

TEST(viewable_list_iterators, fori)
{
	const container c;
	c.addAll(perm4);

	std::vector<int> log;
	for (auto const& item : c)
	{
		log.push_back(item);
	}

	EXPECT_EQ(log, perm4);

	log.clear();
	for (auto it = c.rbegin(); it != c.rend(); ++it)
	{
		log.push_back(*it);
	}
	std::reverse(log.begin(), log.end());
	EXPECT_EQ(log, perm4);
}

TEST(viewable_list_iterators, random_access)
{
	container c;
	c.addAll(perm4);

	EXPECT_EQ(*(c.begin() + 2), 1);
	EXPECT_EQ(*(c.rbegin() + 2), 0);
}

/*TEST(viewable_list_iterators, insert_return_value) {
	container c;
	c.addAll({1, 2, 3, 4});

	container::iterator i = c.add(std::next(c.begin(), 2), 5);
	EXPECT_EQ(5, *i);
	EXPECT_EQ(2, *std::prev(i));
	EXPECT_EQ(3, *std::next(i));
}

TEST(viewable_list_iterators, erase_return_value) {
	container c;
	c.addAll({1, 2, 3, 4});
	container::iterator i = c.remove(std::next(c.begin()));
	EXPECT_EQ(3, *i);
	i = c.remove(i);
	EXPECT_EQ(4, *i);
}*/
