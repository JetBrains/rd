//
// Created by jetbrains on 12.07.2018.
//

#include "ViewableSet.h"

#include "test_util.h"

#include <gtest/gtest.h>

using namespace rd;

TEST (viewable_set, advise) {
	std::unique_ptr<IViewableSet<int>> set = std::make_unique<ViewableSet<int>>();

	std::vector<int> logAdvise;
	std::vector<int> logView1;
	std::vector<int> logView2;
	Lifetime::use([&](Lifetime lt) {
		set->advise(lt, [&](AddRemove kind, int const &v) {
			logAdvise.push_back(kind == AddRemove::ADD ? v : -v);
		});
		set->view(lt, [&](Lifetime inner, int const &v) {
			logView1.push_back(v);
			inner->add_action([&]() { logView1.push_back(-v); });
		});
		set->view(Lifetime::Eternal(), [&](Lifetime inner, int const &v) {
			logView2.push_back(v);
			inner->add_action([&]() { logView2.push_back(-v); });
		});

		EXPECT_TRUE(set->add(1));//1
		//EXPECT_TRUE(set.add(arrayOf(1, 2)) } //1, 2
		EXPECT_FALSE(set->add(1)); //1
		EXPECT_TRUE(set->add(2)); //1, 2

		//EXPECT_TRUE(set.add(arrayOf(1, 2)) } //1, 2
		EXPECT_FALSE(set->add(1)); //1, 2
		EXPECT_FALSE(set->add(2)); //1, 2

//        EXPECT_TRUE{set.removeAll(arrayOf(2, 3))} // 1
		EXPECT_TRUE(set->remove(2)); // 1
		EXPECT_FALSE(set->remove(3)); // 1

		EXPECT_TRUE(set->add(2)); // 1, 2
		EXPECT_FALSE(set->add(2)); // 1, 2

//        EXPECT_TRUE(set.retainAll(arrayOf(2, 3)) // 2
		EXPECT_TRUE(set->remove(1)); // 2
		EXPECT_FALSE(set->remove(3)); // 2
	});

	EXPECT_TRUE(set->add(1));

	std::vector<int> expectedAdvise{1, 2, -2, 2, -1};
	EXPECT_EQ(expectedAdvise, logAdvise);

	std::vector<int> expectedView1{1, 2, -2, 2, -1, -2};
	EXPECT_EQ(expectedView1, logView1);

	std::vector<int> expectedView2{1, 2, -2, 2, -1, 1};
	EXPECT_EQ(expectedView2, logView2);
}

TEST (viewable_set, view) {
	using listOf = std::vector<int>;

	listOf elementsView{2, 0, 1, 8, 3};
	listOf elementsUnView{1, 3, 8, 0, 2};

	size_t C = elementsView.size();

	std::unique_ptr<IViewableSet<int>> set = std::make_unique<ViewableSet<int>>();
	std::vector<std::string> log;
	auto x = Lifetime::use<bool>([&](Lifetime lifetime) {
		set->view(lifetime, [&](Lifetime lt, int const &value) {
					  log.push_back("View " + std::to_string(value));
					  lt->add_action([&]() { log.push_back("UnView " + std::to_string(value)); });
				  }
		);
		for (auto x : elementsView) {
			set->add(x);
		}
		return set->remove(1);
	});
	EXPECT_TRUE(x);
	EXPECT_EQ(C - 1, set->size());

	std::vector<std::string> expected(2 * C);
	for (size_t i = 0; i < C; ++i) {
		expected[i] = "View " + std::to_string(elementsView[i]);
		expected[C + i] = "UnView " + std::to_string(elementsUnView[i]);
	}
	EXPECT_EQ(expected, log);

	log.clear();
	Lifetime::use([&](Lifetime lifetime) {
		set->view(lifetime, [&](Lifetime lt, int const &value) {
					  log.push_back("View " + std::to_string(value));
					  lt->add_action([&]() { log.push_back("UnView " + std::to_string(value)); });
				  }
		);
		set->clear();
	});
	EXPECT_TRUE(set->empty());

	listOf rest_elements{2, 0, 8, 3};

	size_t K = C - 1;
	std::vector<std::string> expected2(2 * K);
	for (size_t i = 0; i < K; ++i) {
		expected2[i] = "View " + std::to_string(rest_elements[i]);
		expected2[K + i] = "UnView " + std::to_string(rest_elements[i]);
	}
	EXPECT_EQ(expected2, log);
}

TEST(viewable_set, add_remove_fuzz) {
	std::unique_ptr<IViewableSet<int> > set = std::make_unique<ViewableSet<int>>();
	std::vector<std::string> log;

	const int C = 10;

	Lifetime::use([&](Lifetime lifetime) {
		set->view(lifetime, [&log](Lifetime lt, int const &value) {
			log.push_back("View " + std::to_string(value));
			lt->add_action([&]() { log.push_back("UnView " + std::to_string(value)); });
		});

		for (int i = 0; i < C; ++i) {
			set->add(i);
		}
	});

	for (int i = 0; i < C; ++i) {
		EXPECT_EQ("View " + std::to_string(i), log[i]);
		EXPECT_EQ("UnView " + std::to_string(C - i - 1), log[C + i]);
	}
}

TEST (viewable_set, move) {
	ViewableSet<int> set1;
	ViewableSet<int> set2(std::move(set1));
}