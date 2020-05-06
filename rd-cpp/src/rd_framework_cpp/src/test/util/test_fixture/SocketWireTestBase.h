#ifndef RD_CPP_SOCKETWIRETESTBASE_H
#define RD_CPP_SOCKETWIRETESTBASE_H

#include "impl/RdProperty.h"
#include "lifetime/LifetimeDefinition.h"
#include "protocol/Protocol.h"
#include "wire/PumpScheduler.h"
#include "wire/WireUtil.h"

#include <gtest/gtest.h>

namespace rd
{
namespace test
{
namespace util
{
class SocketWireTestBase : public ::testing::Test
{
public:
	LifetimeDefinition lifetimeDef{Lifetime::Eternal()};
	LifetimeDefinition socketLifetimeDef{Lifetime::Eternal()};

	Lifetime lifetime = lifetimeDef.lifetime;
	Lifetime socketLifetime = socketLifetimeDef.lifetime;

	PumpScheduler serverScheduler{"server"};
	PumpScheduler clientScheduler{"client"};

	int property_id = 1;

	std::string static_name = "top";

	Protocol server(Lifetime lifetime, uint16_t port = 0);

	Protocol client(Lifetime lifetime, Protocol const& serverProtocol);

	Protocol client(Lifetime lifetime, uint16_t port);

	//    @Before
	void SetUp() override
	{
	}

	void init(Protocol const& serverProtocol, Protocol const& clientProtocol, RdBindableBase const* serverEntity = nullptr,
		RdBindableBase const* clientEntity = nullptr);

	//    @After
	void AfterTest()
	{
	}

	void terminate();

	void checkSchedulersAreEmpty();

	void pump_both();
};
}	 // namespace util
}	 // namespace test
}	 // namespace rd

#endif	  // RD_CPP_SOCKETWIRETESTBASE_H
