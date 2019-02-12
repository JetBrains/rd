//
// Created by jetbrains on 11.07.2018.
//


#include "gtest/gtest.h"

#include "ViewableMap.h"
#include "test_util.h"

using namespace rd;

TEST(viewable_map, advise) {
	std::unique_ptr<IViewableMap<int, int> > map = std::make_unique<ViewableMap<int, int>>();
	map->set(0, 1);
	map->set(1, 1);
	map->set(2, 2);
	map->set(0, 0);
	map->remove(2);

	std::vector<std::string> log_add_remove;
	std::vector<std::string> log_update;
	std::vector<int> log_view;

	Lifetime::use([&](Lifetime lifetime) {
		map->advise_add_remove(lifetime,
							   [&](AddRemove kind, int const &key, int const &value) {
								   log_add_remove.push_back(
										   to_string(kind) + " " + std::to_string(key) + ":" + std::to_string(value));
							   });
		map->advise(lifetime, [&](typename IViewableMap<int, int>::Event entry) {
			log_update.push_back(to_wstring_map_event<int, int>(entry));
		});
		map->view(lifetime, [&](Lifetime inner, const std::pair<int const *, int const *> x) {
			inner->bracket(
					[&log_view, x]() { log_view.push_back(*x.first); },
					[&log_view, x]() { log_view.push_back(-*x.second); }
			);
		});

		lifetime->add_action([&log_add_remove]() { log_add_remove.emplace_back("End"); });

		map->set(0, 1);
		map->set(10, 10);
		map->set(0, 0);
		map->remove(1);

		EXPECT_EQ(0, *map->get(0));
	});
	EXPECT_EQ(arrayListOf(
			{"Add 0:0"_s, "Add 1:1"_s, "Remove 0:0"_s, "Add 0:1"_s, "Add 10:10"_s, "Remove 0:1"_s, "Add 0:0"_s,
			 "Remove 1:1"_s, "End"_s}), log_add_remove);
	EXPECT_EQ(arrayListOf({"Add 0:0"_s, "Add 1:1"_s, "Update 0:1"_s, "Add 10:10"_s, "Update 0:0"_s, "Remove 1"_s}),
			  log_update);
	EXPECT_EQ(arrayListOf({0, 1, -0, 0, 10, -1, 0, -1, /*this events are arguable*/0, -10}), log_view);

	log_add_remove.clear();
	Lifetime::use([&](Lifetime lifetime) {
		map->advise_add_remove(lifetime, [&log_add_remove](AddRemove kind, int const &key, int const &value) {
			log_add_remove.push_back(to_string(kind) + " " + std::to_string(key) + ":" + std::to_string(value));
		});
		map->set(0, 0);

		EXPECT_FALSE(map->empty());
		map->clear();
		EXPECT_TRUE(map->empty());
	});

	EXPECT_EQ(arrayListOf({"Add 0:0"_s, "Add 10:10"_s, "Remove 0:0"_s, "Remove 10:10"_s}), log_add_remove);
}

TEST (viewable_map, view) {
	using listOf = std::vector<int>;

	listOf elementsView{2, 0, 1, 8, 3};
	listOf elementsUnView{1, 3, 8, 0, 2};

	listOf indexesUnView{2, 4, 3, 1, 0};

	size_t C{elementsView.size()};

	std::unique_ptr<IViewableMap<int32_t, int32_t >> map = std::make_unique<ViewableMap<int32_t, int32_t>>();
	std::vector<std::string> log;
	Lifetime::use([&](Lifetime lifetime) {
		map->view(lifetime, [&](Lifetime lt, std::pair<int32_t const *, int32_t const *> value) {
					  log.push_back("View " + to_string(value));
					  lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
				  }
		);
		for (size_t i = 0; i < elementsView.size(); ++i) {
			map->set(i, elementsView[i]);
		}
		map->remove(2);
	});

	EXPECT_EQ(C - 1, map->size());

	std::vector<std::string> expected(2 * C);
	for (size_t i = 0; i < C; ++i) {
		expected[i] = "View (" + std::to_string(i) + ", " + std::to_string(elementsView[i]) + ")";
		expected[C + i] =
				"UnView (" + std::to_string(indexesUnView[i]) + ", " + std::to_string(elementsUnView[i]) + ")";
	}
	EXPECT_EQ(expected, log);
}

TEST(viewable_map, add_remove_fuzz) {
	std::unique_ptr<IViewableMap<int32_t, int32_t> > map(new ViewableMap<int32_t, int32_t>());
	std::vector<std::string> log;

	const int C = 10;

	Lifetime::use([&](Lifetime lifetime) {
		map->view(lifetime, [&log](Lifetime lt, std::pair<int32_t const *, int32_t const *> value) {
			log.push_back("View " + to_string(value));
			lt->add_action([&log, value]() { log.push_back("UnView " + to_string(value)); });
		});

		for (int i = 0; i < C; ++i) {
			map->set(i, 0);
		}
	});

	for (int i = 0; i < C; ++i) {
		EXPECT_EQ("View (" + std::to_string(i) + ", 0)", log[i]);
		EXPECT_EQ("UnView (" + std::to_string(C - i - 1) + ", 0)", log[C + i]);
	}
}

TEST (viewable_map, move) {
	ViewableMap<int, int> set1;
	ViewableMap<int, int> set2(std::move(set1));
}