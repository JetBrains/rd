//
// Created by jetbrains on 06.02.2019.
//

#include "gtest/gtest.h"

#include "RdFrameworkTestBase.h"
#include "RdProperty.h"
#include "AbstractEntity.h"
#include "ArraySerializer.h"
#include "RdSet.h"
#include "RdMap.h"
#include "RdList.h"
#include "RdCall.h"
#include "RdEndpoint.h"


using namespace rd;
using namespace rd::test;
using namespace rd::test::util;

//todo test not only instantiation
using AS = AbstractPolymorphic<AbstractEntity>;

TEST_F(RdFrameworkTestBase, dynamic_polymorphic_signal) {
	using S = ArraySerializer<AbstractPolymorphic<AbstractEntity>>;
	RdSignal<std::vector<Wrapper<AbstractEntity>>, S> _actionExecuted;
}

TEST_F(RdFrameworkTestBase, dynamic_polymorphic_property) {
	RdProperty<AbstractEntity, AS> server_property;
	RdProperty<AbstractEntity, AS> client_property;

//    bindStatic(clientProtocol.get(), client_property, "top");
//    bindStatic(serverProtocol.get(), server_property, "top");
}

TEST_F(RdFrameworkTestBase, dynamic_polymorphic_list) {
	RdList<AbstractEntity, AS> server_list;
	RdList<AbstractEntity, AS> client_list;

//    bindStatic(clientProtocol.get(), client_property, "top");
//    bindStatic(serverProtocol.get(), server_property, "top");
}

TEST_F(RdFrameworkTestBase, dynamic_polymorphic_set) {
	RdSet<AbstractEntity, AS> server_set;
	RdSet<AbstractEntity, AS> client_set;

//    bindStatic(clientProtocol.get(), client_property, "top");
//    bindStatic(serverProtocol.get(), server_property, "top");
}

TEST_F(RdFrameworkTestBase, dynamic_polymorphic_map) {
	RdMap<AbstractEntity, AbstractEntity, AS, AS> server_map;
	RdMap<AbstractEntity, AbstractEntity, AS, AS> client_map;

//    bindStatic(clientProtocol.get(), client_property, "top");
//    bindStatic(serverProtocol.get(), server_property, "top");
}

TEST_F(RdFrameworkTestBase, dynamic_polymorphic_call_endpoint) {
	RdCall<AbstractEntity, AbstractEntity, AS, AS> call;
	RdEndpoint<AbstractEntity, AbstractEntity, AS, AS> endpoint;

//    bindStatic(clientProtocol.get(), client_property, "top");
//    bindStatic(serverProtocol.get(), server_property, "top");
}


