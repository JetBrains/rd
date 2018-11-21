#include "UnrealEngineModel.h"

#include <wire/SocketScheduler.h>
#include <wire/SocketWire.h>

#include <fstream>

int main() {
    std::shared_ptr<IWire> wire;
    std::unique_ptr<IProtocol> clientProtocol;
    SocketScheduler clientScheduler{"server"};

    LifetimeDefinition lifetimeDef{Lifetime::Eternal()};
    LifetimeDefinition socketLifetimeDef{Lifetime::Eternal()};

    Lifetime lifetime{lifetimeDef.lifetime};
    Lifetime socketLifetime{socketLifetimeDef.lifetime};

    std::ofstream outputFile("C:\\temp\\port.txt");

    wire = std::make_shared<SocketWire::Server>(lifetime, &clientScheduler, 0, "TestClient");
    outputFile << (dynamic_cast<SocketWire::Server *>(wire.get()))->port;
    clientProtocol = std::make_unique<Protocol>(Identities(), &clientScheduler, wire);

    {
        UnrealEngineModel model = UnrealEngineModel::create(lifetime, clientProtocol.get());

        model.get_test_connection().advise(lifetime, [](tl::optional<int32_t> const &it) {
            std::cout << "Connection UE: " << it.value();
        });

        model.get_test_string().advise(lifetime, [](tl::optional<std::string> const &it) {
            std::cout << "rdid_filename_to_open changed:" << it.value();
        });

        std::this_thread::sleep_for(std::chrono::minutes(10));
    }
    return 0;
}