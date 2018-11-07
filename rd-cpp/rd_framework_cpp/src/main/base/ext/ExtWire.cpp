//
// Created by jetbrains on 28.09.2018.
//

#include "ExtWire.h"

ExtWire::ExtWire() {
    connected.advise(Lifetime::Eternal(), [this](bool b) {
        if (b) {
            {
                std::lock_guard _(lock);
                while (true) {
                    if (sendQ.empty()) {
                        return;
                    }
                    auto[id, payload] = std::move(sendQ.front());
                    sendQ.pop();
                    realWire->send(id, [payload = std::move(payload)](Buffer const &buffer) {
                        buffer.writeByteArrayRaw(payload);
                    });
                }
            }
        }
    });
}

void ExtWire::advise(Lifetime lifetime, IRdReactive const *entity) const {
    realWire->advise(lifetime, entity);
}

void ExtWire::send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const {
    {
        std::lock_guard _(lock);
        if (!sendQ.empty() || !connected.get()) {
            Buffer buffer;
            writer(buffer);
            sendQ.emplace(id, buffer.getRealArray());
            return;
        }
    }
    realWire->send(id, writer);
}
