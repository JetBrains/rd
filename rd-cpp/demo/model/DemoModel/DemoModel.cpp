#include "DemoModel.h"

#include "MyScalar.h"

#include "DemoRoot.h"

//companion

DemoModel::DemoModelSerializersOwner DemoModel::serializersOwner;

void DemoModel::DemoModelSerializersOwner::registerSerializersCore(rd::Serializers const& serializers)
{
    serializers.registry<MyScalar>();
}

void DemoModel::connect(rd::Lifetime lifetime, rd::IProtocol const * protocol)
{
    DemoRoot::serializersOwner.registry(protocol->get_serializers());
    
    identify(*(protocol->get_identity()), rd::RdId::Null().mix("DemoModel"));
    bind(lifetime, protocol, "DemoModel");
}


//initializer
void DemoModel::initialize()
{
    boolean_property_.optimize_nested = true;
    scalar_.optimize_nested = true;
    list_.optimize_nested = true;
    set_.optimize_nested = true;
    mapLongToString_.optimize_nested = true;
    serializationHash = -7851046433962724830L;
}

//primary ctor
DemoModel::DemoModel(rd::RdProperty<bool, rd::Polymorphic<bool>> boolean_property_, rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> scalar_, rd::RdList<int32_t, rd::Polymorphic<int32_t>> list_, rd::RdSet<int32_t, rd::Polymorphic<int32_t>> set_, rd::RdMap<int64_t, std::wstring, rd::Polymorphic<int64_t>, rd::Polymorphic<std::wstring>> mapLongToString_, rd::RdCall<wchar_t, std::wstring, rd::Polymorphic<wchar_t>, rd::Polymorphic<std::wstring>> call_, rd::RdEndpoint<std::wstring, int32_t, rd::Polymorphic<std::wstring>, rd::Polymorphic<int32_t>> callback_): rd::RdExtBase(), boolean_property_(std::move(boolean_property_)), scalar_(std::move(scalar_)), list_(std::move(list_)), set_(std::move(set_)), mapLongToString_(std::move(mapLongToString_)), call_(std::move(call_)), callback_(std::move(callback_)) { initialize(); }
//reader

//writer

//virtual init
void DemoModel::init(rd::Lifetime lifetime) const
{
    rd::RdExtBase::init(lifetime);
    bindPolymorphic(boolean_property_, lifetime, this, "boolean_property");
    bindPolymorphic(scalar_, lifetime, this, "scalar");
    bindPolymorphic(list_, lifetime, this, "list");
    bindPolymorphic(set_, lifetime, this, "set");
    bindPolymorphic(mapLongToString_, lifetime, this, "mapLongToString");
    bindPolymorphic(call_, lifetime, this, "call");
    bindPolymorphic(callback_, lifetime, this, "callback");
}

//identify
void DemoModel::identify(const rd::Identities &identities, rd::RdId const &id) const
{
    rd::RdBindableBase::identify(identities, id);
    identifyPolymorphic(boolean_property_, identities, id.mix(".boolean_property"));
    identifyPolymorphic(scalar_, identities, id.mix(".scalar"));
    identifyPolymorphic(list_, identities, id.mix(".list"));
    identifyPolymorphic(set_, identities, id.mix(".set"));
    identifyPolymorphic(mapLongToString_, identities, id.mix(".mapLongToString"));
    identifyPolymorphic(call_, identities, id.mix(".call"));
    identifyPolymorphic(callback_, identities, id.mix(".callback"));
}

//getters
rd::RdProperty<bool, rd::Polymorphic<bool>> const & DemoModel::get_boolean_property() const
{
    return boolean_property_;
}
rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> const & DemoModel::get_scalar() const
{
    return scalar_;
}
rd::RdList<int32_t, rd::Polymorphic<int32_t>> const & DemoModel::get_list() const
{
    return list_;
}
rd::RdSet<int32_t, rd::Polymorphic<int32_t>> const & DemoModel::get_set() const
{
    return set_;
}
rd::RdMap<int64_t, std::wstring, rd::Polymorphic<int64_t>, rd::Polymorphic<std::wstring>> const & DemoModel::get_mapLongToString() const
{
    return mapLongToString_;
}
rd::RdCall<wchar_t, std::wstring, rd::Polymorphic<wchar_t>, rd::Polymorphic<std::wstring>> const & DemoModel::get_call() const
{
    return call_;
}
rd::RdEndpoint<std::wstring, int32_t, rd::Polymorphic<std::wstring>, rd::Polymorphic<int32_t>> const & DemoModel::get_callback() const
{
    return callback_;
}

//intern

//equals trait
bool DemoModel::equals(rd::IPolymorphicSerializable const& object) const
{
    auto const &other = dynamic_cast<DemoModel const&>(object);
    return this == &other;
}

//equality operators
bool operator==(const DemoModel &lhs, const DemoModel &rhs){
    return &lhs == &rhs;
}
bool operator!=(const DemoModel &lhs, const DemoModel &rhs){
    return !(lhs == rhs);
}

//hash code trait
