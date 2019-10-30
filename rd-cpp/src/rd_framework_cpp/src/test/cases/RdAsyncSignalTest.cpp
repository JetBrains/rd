#include "RdAsyncTestBase.h"

#include "RdSignal.h"
#include "Void.h"
#include "WireUtil.h"

#include "countdownlatch.hpp"

using namespace rd;
using namespace rd::test;
using namespace rd::test::util;


TEST_F(RdAsyncTestBase, DISABLED_asyncSignalStatic) {
	std::atomic_int32_t acc{0};

	clatch::countdownlatch evt1(3);
	clatch::countdownlatch evt2(1);

	RdSignal<Void> clientSignal;
	clientUiScheduler->queue([&] {
		statics(clientSignal, static_entity_id);
		bindStatic(clientProtocol.get(), clientSignal, static_name);
		clientSignal.async = true;

		evt1.count_down();
		evt1.await();
		clientBgScheduler->queue([&]() {
			clientSignal.fire();
			printf("client fired bg");
		});
		clientSignal.fire();
		printf("client fired ui");
		evt2.count_down();
	});

	RdSignal<Void> serverSignal;
	serverUiScheduler->queue([&]() {
		statics(serverSignal, static_entity_id);
		bindStatic(serverProtocol.get(), serverSignal, static_name);
		serverSignal.async = true;

		LifetimeDefinition::use([&](Lifetime lifetime) {
			serverSignal.advise_on(serverLifetime, serverBgScheduler.get(), [&] {
				printf("server received");
				rd::util::sleep_this_thread(100);
				serverBgScheduler->assert_thread();
				++acc;
			});
			printf("server advise completed");
			evt1.count_down();
			evt1.await();

		});
	});

	EXPECT_EQ(0, acc);
	evt1.count_down();
	evt1.await();


	evt2.await();
	clientUiScheduler->flush();
	clientBgScheduler->flush();

	serverUiScheduler->flush();
	serverBgScheduler->flush();

	EXPECT_EQ(2, acc);
	printf("end of all things");
}