#include "TestModel.h"

#include "SocketWire.h"
//#include "TestScheduler.h"
#include "TestScheduler.h"

#include <fstream>


int main() {
    std::shared_ptr<IWire> wire;
    std::unique_ptr<IProtocol> serverProtocol;
    TestScheduler serverScheduler;

    LifetimeDefinition lifetimeDef{Lifetime::Eternal()};
    LifetimeDefinition socketLifetimeDef{Lifetime::Eternal()};

    Lifetime lifetime{lifetimeDef.lifetime};
    Lifetime socketLifetime{socketLifetimeDef.lifetime};

    std::ofstream outputFile("C:\\temp\\port.txt");

    wire = std::make_shared<SocketWire::Server>(lifetime, &serverScheduler, 0, "TestClient");
    outputFile << (dynamic_cast<SocketWire::Server *>(wire.get()))->port << std::endl;
    serverProtocol = std::make_unique<Protocol>(Identities(Identities::SERVER), &serverScheduler, wire);

    {
        TestModel model = TestModel::create(lifetime, serverProtocol.get());

        int number = 2018;
        std::wstring str;
        try {
            str = model.get_test().sync(number);
        } catch (std::exception const &e) {
            std::cerr << "ERROR:" << e.what() << std::endl;
        }

        std::cout << number << "------------------>";
        std::wcout << str << std::endl;

        std::this_thread::sleep_for(std::chrono::minutes(10));
    }
    return 0;
}