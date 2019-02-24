//
// Created by jetbrains on 06.02.2019.
//

#include "gtest/gtest.h"

#include "RdFrameworkTestBase.h"
#include "RdFrameworkDynamicPolymorphicTestBase.h"

#include "AbstractEntity.h"
#include "ConcreteEntity.h"
#include "FakeEntity.h"
#include "ArraySerializer.h"
#include "RdProperty.h"
#include "RdSet.h"
#include "RdMap.h"
#include "RdList.h"
#include "RdCall.h"
#include "RdEndpoint.h"
#include "test_util.h"


using namespace rd;
using namespace rd::test;
using namespace rd::test::util;

using S = AbstractPolymorphic<AbstractEntity>;

using DSignal = RdSignal<AbstractEntity, S>;
using DSignalTest = RdFrameworkDynamicPolymorphicTestBase<DSignal>;

TEST_F(DSignalTest, dynamic_polymorphic_signal) {
	ConcreteEntity value_a{L"A"};
	std::vector<std::wstring> log;
	std::vector<size_t> hc;
	server_entity.advise(serverLifetime, [&hc, &log](AbstractEntity const &value) {
		log.push_back(value.get_filePath());
		hc.push_back(value.hashCode());
	});
	client_entity.fire(value_a);
	EXPECT_EQ(log[0], value_a.get_filePath());
	client_entity.fire(value_a);
	EXPECT_EQ(log[1], value_a.get_filePath());
	serverLifetimeDef.terminate();

	client_entity.fire(value_a);
	EXPECT_EQ(log.size(), 2);

	EXPECT_EQ(hc[0], value_a.hashCode());
}

using DProperty = RdProperty<AbstractEntity, S>;
using DPropertyTest = RdFrameworkDynamicPolymorphicTestBase<DProperty>;

TEST_F(DPropertyTest, dynamic_polymorphic_property) {
	ConcreteEntity value_a{L"A"};
	ConcreteEntity value_b{L"B"};

	std::vector<std::wstring> log;

	server_entity.advise(clientLifetime, [&log](AbstractEntity const &entity) {
		log.push_back(entity.get_filePath());
	});
	server_entity.set(value_a);
	EXPECT_EQ(server_entity.get(), value_a);
	EXPECT_NE(server_entity.get(), value_b);

	client_entity.set(value_a);
}

using DList = RdList<AbstractEntity, S>;
using DListTest = RdFrameworkDynamicPolymorphicTestBase<DList>;

TEST_F(DListTest, dynamic_polymorphic_list) {
	std::vector<std::string> log;
	Lifetime::use([this, &log](Lifetime lifetime) {
		server_entity.advise(lifetime, [&log](DList::Event e) {
			auto string = to_string_list_event<AbstractEntity>(e);
			if (e.get_new_value() != nullptr) {
				string += to_string(e.get_new_value()->get_filePath());
			}
			log.push_back(string);
		});

		ConcreteEntity value_a{L"A"};
		ConcreteEntity value_b{L"B"};
		client_entity.add(value_a);
		client_entity.add(value_b);

		server_entity.remove(value_a);
	});

	std::vector<std::string> expected{"Add 0:A", "Add 1:B", "Remove 0"};
	EXPECT_EQ(expected, log);
}

using DSet = RdSet<AbstractEntity, S>;
using DSetTest = RdFrameworkDynamicPolymorphicTestBase<DSet>;

TEST_F(DSetTest, dynamic_polymorphic_set) {
	std::vector<std::string> log;
	Lifetime::use([this, &log](Lifetime lifetime) {
		server_entity.advise(lifetime, [&log](DSet::Event e) {
			auto x = e.value->get_filePath();
			log.push_back(to_string_set_event<AbstractEntity>(e) + to_string(x));
		});

		ConcreteEntity value_a{L"A"};
		ConcreteEntity value_b{L"B"};
		ConcreteEntity value_c{L"C"};

		client_entity.add(value_c);
		client_entity.add(value_a);
		client_entity.add(value_b);

		client_entity.remove(value_a);

		server_entity.clear();
	});

	std::vector<std::string> expected{"Add C", "Add A", "Add B", "Remove A", "Remove C", "Remove B"};
	EXPECT_EQ(expected, log);
}

using DMap = RdMap<AbstractEntity, AbstractEntity, S, S>;
using DMapTest = RdFrameworkDynamicPolymorphicTestBase<DMap>;

TEST_F(DMapTest, dynamic_polymorphic_map) {
	std::vector<std::string> log;
	Lifetime::use([this, &log](Lifetime lifetime) {
		server_entity.advise(lifetime, [&log](DMap::Event e) {
			auto x = to_string_map_event<AbstractEntity, AbstractEntity>(e)
					 + to_string(e.get_key()->get_filePath());
			log.push_back(x);
		});

		ConcreteEntity key_1{L"1"};
		ConcreteEntity key_2{L"2"};
		ConcreteEntity key_3{L"3"};

		ConcreteEntity value_a{L"A"};
		ConcreteEntity value_b{L"B"};
		ConcreteEntity value_c{L"C"};

		client_entity.set(key_3, value_c);
		client_entity.set(key_1, value_a);
		client_entity.set(key_2, value_b);

		EXPECT_FALSE(client_entity.remove(value_a)); //remove by value is wrong
		EXPECT_EQ(server_entity.size(), 3);

		EXPECT_EQ(**server_entity.remove(key_1), value_a);

		server_entity.clear();
	});

	std::vector<std::string> expected{"Add :3", "Add :1", "Add :2", "Remove 1", "Remove 3", "Remove 2"};
	EXPECT_EQ(expected, log);
}

using DEndpoint = RdEndpoint<AbstractEntity, AbstractEntity, S, S>;
using DCall = RdCall<AbstractEntity, AbstractEntity, S, S>;
using DTaskTest = RdFrameworkDynamicPolymorphicTestBase<DEndpoint, DCall>;

TEST_F(DTaskTest, dynamic_polymorphic_call_endpoint) {
	serverProtocol->serializers.registry<FakeEntity>();
	clientProtocol->serializers.registry<FakeEntity>();

	server_entity.set([](AbstractEntity const &value) -> Wrapper<AbstractEntity> {
		if (value.type_name() == "ConcreteEntity") {
			ConcreteEntity res{value.get_filePath()};
			return Wrapper<AbstractEntity>{std::move(res)};
		} else if (value.type_name() == "FakeEntity") {
			FakeEntity res{value.get_filePath()};
			return Wrapper<AbstractEntity>{std::move(res)};
		} else {
			throw std::invalid_argument("wrong type");
		}
		//todo resolve types
	});
	ConcreteEntity value_a{L"A"};
	Wrapper<AbstractEntity> res = client_entity.sync(value_a);
	EXPECT_EQ(*res, value_a);

	FakeEntity fake_entity{L"A"};
	auto task = client_entity.start(fake_entity, &clientScheduler);
	auto task_result = task.value_or_throw();
	Wrapper<AbstractEntity> unwrap = task_result.unwrap();
	EXPECT_NE(*unwrap, value_a);

	EXPECT_FALSE(task_result.unwrap()); //the value was stolen by previous unwrap
}
