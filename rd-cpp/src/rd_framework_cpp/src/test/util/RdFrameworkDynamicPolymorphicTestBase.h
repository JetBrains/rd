#ifndef RdFrameworkDynamicPolymorphicTestBase_H
#define RdFrameworkDynamicPolymorphicTestBase_H

#include "RdFrameworkTestBase.h"
#include "RdReactiveBase.h"

#include <type_traits>
#include "ConcreteEntity.h"

namespace rd {
	namespace test {
		namespace util {
			template<typename S, typename C = S, typename T = ConcreteEntity,
					typename = typename std::enable_if<std::is_base_of<RdReactiveBase, S>::value>::type,
					typename = typename std::enable_if<std::is_base_of<RdReactiveBase, C>::value>::type
			>

			class RdFrameworkDynamicPolymorphicTestBase : public RdFrameworkTestBase {
			public:
				S server_entity;
				C client_entity;
			protected:
				void SetUp() override {
					serverProtocol->serializers.registry<T>();
					clientProtocol->serializers.registry<T>();

					int entity_id = 1;

					statics(client_entity, entity_id);
					statics(server_entity, entity_id);

					bindStatic(serverProtocol.get(), server_entity, "top");
					bindStatic(clientProtocol.get(), client_entity, "top");
				}

				void TearDown() override {
					Test::TearDown();
				}
			};
		}
	}
}

#endif // RdFrameworkDynamicPolymorphicTestBase_H