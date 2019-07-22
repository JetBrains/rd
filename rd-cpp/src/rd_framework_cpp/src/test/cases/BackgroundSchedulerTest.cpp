#include <gtest/gtest.h>

#include "SingleThreadScheduler.h"
#include "WireUtil.h"
#include "LifetimeDefinition.h"

using namespace rd;

TEST(BackgroundSchedulerTest, Simple) {
	LifetimeDefinition definition{false};
	Lifetime lifetime = definition.lifetime;
	rd::SingleThreadScheduler s(lifetime, "test");
	EXPECT_FALSE(s.is_active());
//	EXPECT_THROW(s.assert_thread(), std::exception);

	std::atomic_int32_t tasks_executed{0};
	s.queue([&]() {
		util::sleep_this_thread(100);
		tasks_executed++;
	});
	s.queue([&]() {
		tasks_executed++;
		s.assert_thread();
	});
	EXPECT_EQ(0, tasks_executed);

	s.flush();
	EXPECT_EQ(2, tasks_executed);

	s.queue([]() {
		throw std::invalid_argument("");
	});
	s.queue([&]() {
		tasks_executed++;
	});

	s.flush();
	EXPECT_EQ(3, tasks_executed);
}