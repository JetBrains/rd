#include "SocketWireTestBase.h"

#include "SocketWire.h"
#include "Identities.h"

namespace rd {
	namespace test {
		namespace util {
			Protocol SocketWireTestBase::server(Lifetime lifetime, uint16_t port) {
				std::shared_ptr<IWire> wire = std::make_shared<SocketWire::Server>(std::move(lifetime),
																				   &serverScheduler, port,
																				   "TestServer");
				return Protocol(Identities::SERVER, &serverScheduler, std::move(wire));
			}

			Protocol SocketWireTestBase::client(Lifetime lifetime, Protocol const &serverProtocol) {
				auto const *server = dynamic_cast<SocketWire::Server const *>(serverProtocol.wire.get());
				std::shared_ptr<IWire> wire =
						std::make_shared<SocketWire::Client>(std::move(lifetime), &clientScheduler, server->port,
															 "TestClient");
				return Protocol(Identities::CLIENT, &clientScheduler, std::move(wire));
			}

			Protocol SocketWireTestBase::client(Lifetime lifetime, uint16_t port) {
				std::shared_ptr<IWire> wire =
						std::make_shared<SocketWire::Client>(std::move(lifetime), &clientScheduler, port, "TestClient");
				return Protocol(Identities::CLIENT, &clientScheduler, std::move(wire));
			}

			void SocketWireTestBase::init(Protocol const &serverProtocol, Protocol const &clientProtocol,
										  const RdBindableBase *serverEntity, const RdBindableBase *clientEntity) {
				if (serverEntity) {
					statics(*serverEntity, property_id);
					serverEntity->bind(lifetime, &serverProtocol, "top");
				}

				if (clientEntity) {
					statics(*clientEntity, property_id);
					clientEntity->bind(lifetime, &clientProtocol, "top");
				}
			}

			void SocketWireTestBase::terminate() {
				socketLifetimeDef.terminate();
				lifetimeDef.terminate();
			}

			void SocketWireTestBase::checkSchedulersAreEmpty() {
				rd::util::sleep_this_thread(200);
				EXPECT_TRUE(clientScheduler.messages.empty());
				EXPECT_TRUE(serverScheduler.messages.empty());
			}
		}
	}
}
