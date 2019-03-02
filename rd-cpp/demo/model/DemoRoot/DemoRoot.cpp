#include "DemoRoot.h"


#include "DemoRoot.h"

//companion

DemoRoot::DemoRootSerializersOwner DemoRoot::serializersOwner;

void DemoRoot::DemoRootSerializersOwner::registerSerializersCore(rd::Serializers const& serializers)
{
}

void DemoRoot::connect(rd::Lifetime lifetime, rd::IProtocol const * protocol)
{
    DemoRoot::serializersOwner.registry(protocol->serializers);
    
    identify(*(protocol->identity), rd::RdId::Null().mix("DemoRoot"));
    bind(lifetime, protocol, "DemoRoot");
}


//initializer
void DemoRoot::initialize()
{
    serializationHash = 2990580803186469991L;
}

//primary ctor
DemoRoot::DemoRoot(): rd::RdExtBase() { initialize(); }
//reader

//writer

//virtual init
void DemoRoot::init(rd::Lifetime lifetime) const
{
    rd::RdExtBase::init(lifetime);
}

//identify
void DemoRoot::identify(const rd::Identities &identities, rd::RdId const &id) const
{
    rd::RdBindableBase::identify(identities, id);
}

//getters

//intern

//equals trait
bool DemoRoot::equals(rd::IPolymorphicSerializable const& object) const
{
    auto const &other = dynamic_cast<DemoRoot const&>(object);
    return this == &other;
}

//equality operators
bool operator==(const DemoRoot &lhs, const DemoRoot &rhs){
    return &lhs == &rhs;
}
bool operator!=(const DemoRoot &lhs, const DemoRoot &rhs){
    return !(lhs == rhs);
}

//hash code trait
