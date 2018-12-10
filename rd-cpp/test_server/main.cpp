#include "UnrealEngineModel.h"

#include "SocketWire.h"
//#include "TestScheduler.h"
#include "TestScheduler.h"

#include <fstream>


int main() {
    std::shared_ptr<SocketWire::Server> wire;
    std::unique_ptr<IProtocol> serverProtocol;
    TestScheduler serverScheduler;

    LifetimeDefinition lifetimeDef{Lifetime::Eternal()};
    LifetimeDefinition socketLifetimeDef{Lifetime::Eternal()};

    Lifetime lifetime{lifetimeDef.lifetime};
    Lifetime socketLifetime{socketLifetimeDef.lifetime};

    std::ofstream outputFile("C:\\temp\\port.txt");

    wire = std::make_shared<SocketWire::Server>(lifetime, &serverScheduler, 0, "TestClient");
    outputFile << wire.get()->port << std::endl;
    serverProtocol = std::make_unique<Protocol>(Identities(Identities::SERVER), &serverScheduler, wire);

    {
        UnrealEngineModel model = UnrealEngineModel::create(lifetime, serverProtocol.get());

        model.get_test_connection().advise(lifetime, [](tl::optional<int32_t> const &it) {
            std::cout << "Connection UE: " << to_string(it) << std::endl;
        });

        model.get_filename_to_open().advise(lifetime, [](tl::optional<std::wstring> const &it) {
            std::cout << "rdid_filename_to_open changed:" << to_string(it) << std::endl;
        });

        std::this_thread::sleep_for(std::chrono::minutes(10));
    }
    return 0;
}