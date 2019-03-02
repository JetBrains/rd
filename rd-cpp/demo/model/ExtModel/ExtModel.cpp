#include "ExtModel.h"


#include "DemoRoot.h"

//companion

ExtModel::ExtModelSerializersOwner ExtModel::serializersOwner;

void ExtModel::ExtModelSerializersOwner::registerSerializersCore(rd::Serializers const& serializers)
{
}



//extension
ExtModel const & ExtModel::getOrCreateExtensionOf(DemoModel & pointcut)
{
    return pointcut.getOrCreateExtension<ExtModel>("extModel");
    
}

//initializer
void ExtModel::initialize()
{
    serializationHash = 2364843396187734L;
}

//primary ctor
ExtModel::ExtModel(rd::RdSignal<rd::Void, rd::Polymorphic<rd::Void>> checker_): rd::RdExtBase(), checker_(std::move(checker_)) { initialize(); }
//reader

//writer

//virtual init
void ExtModel::init(rd::Lifetime lifetime) const
{
    rd::RdExtBase::init(lifetime);
    bindPolymorphic(checker_, lifetime, this, "checker");
}

//identify
void ExtModel::identify(const rd::Identities &identities, rd::RdId const &id) const
{
    rd::RdBindableBase::identify(identities, id);
    identifyPolymorphic(checker_, identities, id.mix(".checker"));
}

//getters
rd::RdSignal<rd::Void, rd::Polymorphic<rd::Void>> const & ExtModel::get_checker() const
{
    return checker_;
}

//intern

//equals trait
bool ExtModel::equals(rd::IPolymorphicSerializable const& object) const
{
    auto const &other = dynamic_cast<ExtModel const&>(object);
    return this == &other;
}

//equality operators
bool operator==(const ExtModel &lhs, const ExtModel &rhs){
    return &lhs == &rhs;
}
bool operator!=(const ExtModel &lhs, const ExtModel &rhs){
    return !(lhs == rhs);
}

//hash code trait
