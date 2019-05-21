#ifndef RdFrameworkDynamicPolymorphicTestBase_H
#define RdFrameworkDynamicPolymorphicTestBase_H

#include "ConcreteEntity.h"
#include "RdFrameworkTestBase.h"
#include "RdReactiveBase.h"

#include "core_traits.h"

#include <type_traits>

namespace rd {
	namespace test {
		namespace util {
			template<typename S, typename C = S, typename T = ConcreteEntity,
					typename = typename std::enable_if_t<::rd::util::is_base_of_v<RdReactiveBase, S>>,
					typename = typename std::enable_if_t<::rd::util::is_base_of_v<RdReactiveBase, C>>
			>
			class RdFrameworkDynamicPolymorphicTestBase : public RdFrameworkTestBase {
			public:
				S server_entity;
				C client_entity;
			protected:
				void SetUp() override {
					serverProtocol->serializers->registry<T>();
					clientProtocol->serializers->registry<T>();

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