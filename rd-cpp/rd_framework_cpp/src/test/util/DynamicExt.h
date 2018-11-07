#include <utility>

//
// Created by jetbrains on 01.10.2018.
//

#ifndef RD_CPP_DYNAMICEXT_H
#define RD_CPP_DYNAMICEXT_H


#include "ext/RdExtBase.h"
#include "RdProperty.h"

class DynamicExt : public RdExtBase, public ISerializable {
public:
    RdProperty<std::string> bar = RdProperty<std::string>("");
    std::string debugName;

    DynamicExt();

    DynamicExt(RdProperty<std::string> bar, std::string debugName);

    DynamicExt(std::string const &bar, std::string const &debugName);

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override;

    /*virtual void bind(Lifetime lf, IRdDynamic const *parent, std::string const &name) const;

    virtual void identify(IIdentities const &identities, RdId id) const;*/

    static void registry(IProtocol *protocol);
};


#endif //RD_CPP_DYNAMICEXT_H
