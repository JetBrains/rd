//
// Created by jetbrains on 24.07.2018.
//

#ifndef RD_CPP_RDEXTBASE_H
#define RD_CPP_RDEXTBASE_H

#include "RdReactiveBase.h"
#include "ExtWire.h"

class RdExtBase : public RdReactiveBase {
public:
    enum class ExtState {
        Ready,
        ReceivedCounterpart,
        Disconnected
    };

    std::shared_ptr<ExtWire> extWire = std::make_shared<ExtWire>();
    mutable std::shared_ptr<IProtocol> extProtocol/* = nullptr*/;
    mutable int64_t serializationHash = 0;

    const IProtocol *const get_protocol() const override;

    void init(Lifetime lifetime) const override;

    void on_wire_received(Buffer buffer) const override;

    void sendState(IWire const &wire, RdExtBase::ExtState state) const;

    void traceMe(const Logger &logger, std::string const &message) const;
};


#endif //RD_CPP_RDEXTBASE_H
