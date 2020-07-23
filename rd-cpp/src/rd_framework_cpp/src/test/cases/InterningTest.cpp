#include <gtest/gtest.h>

#include "InterningTestBase.h"
#include "InterningNestedTestStringModel.Generated.h"
#include "InterningNestedTestModel.Generated.h"
#include "InterningTestModel.Generated.h"
#include "PropertyHolderWithInternRoot.h"

using namespace rd;
using namespace rd::test;
using namespace rd::test::util;

TEST_F(InterningTestBase, testServerToClient)
{
	testIntern(false, false);
}

TEST_F(InterningTestBase, testClientToServer)
{
	testIntern(true, true);
}

TEST_F(InterningTestBase, testServerThenClientMixed)
{
	testIntern(false, true);
}

TEST_F(InterningTestBase, testClientThenServerMixed)
{
	testIntern(true, false);
}

TEST_F(InterningTestBase, testServerThenClientMixedAndReversed)
{
	testIntern(false, true, true);
}

TEST_F(InterningTestBase, testClientThenServerMixedAndReversed)
{
	testIntern(true, false, true);
}

TEST_F(InterningTestBase, testLateBindOfObjectWithContent)
{
	auto serverProperty = RdProperty<InterningTestModel>();
	serverProperty.slave();
	auto clientProperty = RdProperty<InterningTestModel>();

	serverProperty.identify(*serverIdentities, RdId(1L));
	clientProperty.identify(*clientIdentities, RdId(1L));

	bindStatic(serverProtocol.get(), serverProperty, static_name);
	bindStatic(clientProtocol.get(), clientProperty, static_name);

	auto serverModel = InterningTestModel(L"");

	for_each([&](int32_t const& k, std::wstring const& v) { serverModel.get_issues().set(k, WrappedStringModel(v)); });

	serverProperty.set(std::move(serverModel));

	auto const& clientModel = clientProperty.get();

	for_each([&](int32_t const& k, std::wstring const& v) {
		auto value = clientModel.get_issues().get(k);
		EXPECT_NE(nullptr, value);
		EXPECT_EQ(v, value->get_text());
	});

	AfterTest();
}

TEST_F(InterningTestBase, testServerToClientProtocolLevel)
{
	testProtocolLevelIntern(false, false);
}

TEST_F(InterningTestBase, testClientToServerProtocolLevel)
{
	testProtocolLevelIntern(true, true);
}

TEST_F(InterningTestBase, testServerThenClientMixedProtocolLevel)
{
	testProtocolLevelIntern(false, true);
}

TEST_F(InterningTestBase, testClientThenServerMixedProtocolLevel)
{
	testProtocolLevelIntern(true, false);
}

TEST_F(InterningTestBase, testServerThenClientMixedAndReversedProtocolLevel)
{
	testProtocolLevelIntern(false, true, true);
}

TEST_F(InterningTestBase, testClientThenServerMixedAndReversedProtocolLevel)
{
	testProtocolLevelIntern(true, false, true);
}

TEST_F(InterningTestBase, testNestedInternedObjects)
{
	serverProtocol->get_serializers().registry<InterningNestedTestModel>();
	clientProtocol->get_serializers().registry<InterningNestedTestModel>();

	using IS = InternedSerializer<Polymorphic<InterningNestedTestModel>, rd::util::getPlatformIndependentHash("TestInternScope")>;

	auto server_property = RdProperty<InterningNestedTestModel, IS>();
	statics(server_property, 1).slave();
	auto client_property = RdProperty<InterningNestedTestModel, IS>();
	statics(client_property, 1);

	auto server_property_holder = PropertyHolderWithInternRoot<InterningNestedTestModel, IS>(std::move(server_property));
	auto client_property_holder = PropertyHolderWithInternRoot<InterningNestedTestModel, IS>(std::move(client_property));

	server_property_holder.mySerializationContext =
		serverProtocol->get_serialization_context().withInternRootsHere(server_property_holder, {"TestInternScope"});
	client_property_holder.mySerializationContext =
		clientProtocol->get_serialization_context().withInternRootsHere(client_property_holder, {"TestInternScope"});

	auto const& server_property_view = server_property_holder.property;
	auto const& client_property_view = client_property_holder.property;

	bindStatic(serverProtocol.get(), server_property_holder, static_name);
	bindStatic(clientProtocol.get(), client_property_holder, static_name);

	auto testValue = InterningNestedTestModel(
		L"extremelyLongString", InterningNestedTestModel(L"middle", InterningNestedTestModel(L"bottom", nullopt)));

	const auto first_send_bytes = measureBytes(serverProtocol.get(), [&] {
		server_property_view.set(testValue);
		EXPECT_EQ(testValue, client_property_view.get()) << "Received value should be the same as sent one";
	});

	const auto second_send_bytes = measureBytes(serverProtocol.get(), [&] {
		server_property_view.set(*testValue.get_inner());
		EXPECT_EQ(testValue.get_inner(), client_property_view.get()) << "Received value should be the same as sent one";
	});

	const auto third_send_bytes = measureBytes(serverProtocol.get(), [&] {
		server_property_view.set(testValue);
		EXPECT_EQ(testValue, client_property_view.get()) << "Received value should be the same as sent one";
	});

	const auto sum_lengths = [](InterningNestedTestModel const& value) -> int64_t {
		auto rec = [](InterningNestedTestModel const& value, auto& impl) mutable -> int64_t {
			int64_t x = (value.get_inner().has_value() ? impl(*value.get_inner(), impl) : 0);
			return value.get_value().length() * 2 + 4 + x;
		};

		return rec(value, rec);
	};

	EXPECT_EQ(second_send_bytes, third_send_bytes) << "Sending a single interned object should take the same amount of bytes";
	EXPECT_TRUE(third_send_bytes <= first_send_bytes - sum_lengths(testValue));

	AfterTest();
}

TEST_F(InterningTestBase, testNestedInternedObjectsOnSameData)
{
	serverProtocol->get_serializers().registry<InterningNestedTestModel>();
	clientProtocol->get_serializers().registry<InterningNestedTestModel>();

	RdProperty<InterningNestedTestStringModel> server_property;
	RdProperty<InterningNestedTestStringModel> client_property;

	statics(server_property, 1).slave();
	statics(client_property, 1);

	PropertyHolderWithInternRoot<InterningNestedTestStringModel> serverPropertyHolder(std::move(server_property));
	PropertyHolderWithInternRoot<InterningNestedTestStringModel> clientPropertyHolder(std::move(client_property));

	serverPropertyHolder.mySerializationContext =
		serverProtocol->get_serialization_context().withInternRootsHere(serverPropertyHolder, {"TestInternScope"});
	clientPropertyHolder.mySerializationContext =
		clientProtocol->get_serialization_context().withInternRootsHere(clientPropertyHolder, {"TestInternScope"});

	auto const& server_property_view = serverPropertyHolder.property;
	auto const& client_property_view = clientPropertyHolder.property;

	bindStatic(serverProtocol.get(), serverPropertyHolder, static_name);
	bindStatic(clientProtocol.get(), clientPropertyHolder, static_name);

	std::wstring sameString = L"thisStringHasANiceLengthThatWillDominateBytesSentCount";
	auto nested0 =
		wrapper::make_wrapper<InterningNestedTestStringModel>(sameString, Wrapper<InterningNestedTestStringModel>(nullptr));
	auto nested1 = wrapper::make_wrapper<InterningNestedTestStringModel>(sameString, *nested0);
	auto testValue = wrapper::make_wrapper<InterningNestedTestStringModel>(sameString, *nested1);
	// todo wrapper nullopt

	auto first_send_bytes = measureBytes(serverProtocol.get(), [&] {
		server_property_view.set(*testValue);
		EXPECT_EQ(*testValue, client_property_view.get());
	});

	// expected send: string + 4 bytes length + 4 bytes id + 8+4 bytes polymorphic write, 3 bytes nullability, 3x 4byte ids, 4 bytes
	// property version
	int32_t send_target = static_cast<int32_t>(sameString.length()) * 2 + 4 + 4 + 8 + 4 + 3 + 4 * 3 + 4;
	RD_ASSERT_MSG(
		first_send_bytes <= send_target, "Sent " + std::to_string(first_send_bytes) + ", expected " + std::to_string(send_target))

	AfterTest();
}
