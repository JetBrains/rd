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

					statics(client_entity, static_entity_id);
					statics(server_entity, static_entity_id);

					bindStatic(serverProtocol.get(), server_entity, static_name);
					bindStatic(clientProtocol.get(), client_entity, static_name);
				}

				void TearDown() override {
					Test::TearDown();
				}
			};
		}
	}
}

#endif // RdFrameworkDynamicPolymorphicTestBase_H