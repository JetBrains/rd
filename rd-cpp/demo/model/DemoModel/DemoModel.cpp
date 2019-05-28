#include "DemoModel.h"

#include "MyEnum.h"
#include "MyScalar.h"
#include "Derived.h"
#include "Base_Unknown.h"

#include "DemoRoot.h"
namespace demo {
    
    //companion
    
    DemoModel::DemoModelSerializersOwner const DemoModel::serializersOwner;
    
    void DemoModel::DemoModelSerializersOwner::registerSerializersCore(rd::Serializers const& serializers) const
    {
        serializers.registry<MyScalar>();
        serializers.registry<Derived>();
        serializers.registry<Base_Unknown>();
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
        boolean_array_.optimize_nested = true;
        scalar_.optimize_nested = true;
        ubyte_.optimize_nested = true;
        ubyte_array_.optimize_nested = true;
        list_.optimize_nested = true;
        set_.optimize_nested = true;
        mapLongToString_.optimize_nested = true;
        interned_string_.optimize_nested = true;
        polymorphic_.optimize_nested = true;
        enum_.optimize_nested = true;
        serializationHash = -6563454397007024222L;
    }
    
    //primary ctor
    DemoModel::DemoModel(rd::RdProperty<bool, rd::Polymorphic<bool>> boolean_property_, rd::RdProperty<std::vector<bool>, DemoModel::__BoolArraySerializer> boolean_array_, rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> scalar_, rd::RdProperty<uint8_t, rd::Polymorphic<uint8_t>> ubyte_, rd::RdProperty<std::vector<uint8_t>, DemoModel::__UByteArraySerializer> ubyte_array_, rd::RdList<int32_t, rd::Polymorphic<int32_t>> list_, rd::RdSet<int32_t, rd::Polymorphic<int32_t>> set_, rd::RdMap<int64_t, std::wstring, rd::Polymorphic<int64_t>, rd::Polymorphic<std::wstring>> mapLongToString_, rd::RdCall<wchar_t, std::wstring, rd::Polymorphic<wchar_t>, rd::Polymorphic<std::wstring>> call_, rd::RdEndpoint<std::wstring, int32_t, rd::Polymorphic<std::wstring>, rd::Polymorphic<int32_t>> callback_, rd::RdProperty<std::wstring, DemoModel::__StringInternedAtProtocolSerializer> interned_string_, rd::RdProperty<Base, rd::AbstractPolymorphic<Base>> polymorphic_, rd::RdProperty<MyEnum, rd::Polymorphic<MyEnum>> enum_) :
    rd::RdExtBase()
    ,boolean_property_(std::move(boolean_property_)), boolean_array_(std::move(boolean_array_)), scalar_(std::move(scalar_)), ubyte_(std::move(ubyte_)), ubyte_array_(std::move(ubyte_array_)), list_(std::move(list_)), set_(std::move(set_)), mapLongToString_(std::move(mapLongToString_)), call_(std::move(call_)), callback_(std::move(callback_)), interned_string_(std::move(interned_string_)), polymorphic_(std::move(polymorphic_)), enum_(std::move(enum_))
    {
        initialize();
    }
    
    //secondary constructor
    
    //default ctors and dtors
    DemoModel::DemoModel()
    {
        initialize();
    }
    
    //reader
    
    //writer
    
    //virtual init
    void DemoModel::init(rd::Lifetime lifetime) const
    {
        rd::RdExtBase::init(lifetime);
        bindPolymorphic(boolean_property_, lifetime, this, "boolean_property");
        bindPolymorphic(boolean_array_, lifetime, this, "boolean_array");
        bindPolymorphic(scalar_, lifetime, this, "scalar");
        bindPolymorphic(ubyte_, lifetime, this, "ubyte");
        bindPolymorphic(ubyte_array_, lifetime, this, "ubyte_array");
        bindPolymorphic(list_, lifetime, this, "list");
        bindPolymorphic(set_, lifetime, this, "set");
        bindPolymorphic(mapLongToString_, lifetime, this, "mapLongToString");
        bindPolymorphic(call_, lifetime, this, "call");
        bindPolymorphic(callback_, lifetime, this, "callback");
        bindPolymorphic(interned_string_, lifetime, this, "interned_string");
        bindPolymorphic(polymorphic_, lifetime, this, "polymorphic");
        bindPolymorphic(enum_, lifetime, this, "enum");
    }
    
    //identify
    void DemoModel::identify(const rd::Identities &identities, rd::RdId const &id) const
    {
        rd::RdBindableBase::identify(identities, id);
        identifyPolymorphic(boolean_property_, identities, id.mix(".boolean_property"));
        identifyPolymorphic(boolean_array_, identities, id.mix(".boolean_array"));
        identifyPolymorphic(scalar_, identities, id.mix(".scalar"));
        identifyPolymorphic(ubyte_, identities, id.mix(".ubyte"));
        identifyPolymorphic(ubyte_array_, identities, id.mix(".ubyte_array"));
        identifyPolymorphic(list_, identities, id.mix(".list"));
        identifyPolymorphic(set_, identities, id.mix(".set"));
        identifyPolymorphic(mapLongToString_, identities, id.mix(".mapLongToString"));
        identifyPolymorphic(call_, identities, id.mix(".call"));
        identifyPolymorphic(callback_, identities, id.mix(".callback"));
        identifyPolymorphic(interned_string_, identities, id.mix(".interned_string"));
        identifyPolymorphic(polymorphic_, identities, id.mix(".polymorphic"));
        identifyPolymorphic(enum_, identities, id.mix(".enum"));
    }
    
    //getters
    rd::RdProperty<bool, rd::Polymorphic<bool>> const & DemoModel::get_boolean_property() const
    {
        return boolean_property_;
    }
    rd::RdProperty<std::vector<bool>, DemoModel::__BoolArraySerializer> const & DemoModel::get_boolean_array() const
    {
        return boolean_array_;
    }
    rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> const & DemoModel::get_scalar() const
    {
        return scalar_;
    }
    rd::RdProperty<uint8_t, rd::Polymorphic<uint8_t>> const & DemoModel::get_ubyte() const
    {
        return ubyte_;
    }
    rd::RdProperty<std::vector<uint8_t>, DemoModel::__UByteArraySerializer> const & DemoModel::get_ubyte_array() const
    {
        return ubyte_array_;
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
    rd::RdProperty<std::wstring, DemoModel::__StringInternedAtProtocolSerializer> const & DemoModel::get_interned_string() const
    {
        return interned_string_;
    }
    rd::RdProperty<Base, rd::AbstractPolymorphic<Base>> const & DemoModel::get_polymorphic() const
    {
        return polymorphic_;
    }
    rd::RdProperty<MyEnum, rd::Polymorphic<MyEnum>> const & DemoModel::get_enum() const
    {
        return enum_;
    }
    
    //intern
    
    //equals trait
    
    //equality operators
    bool operator==(const DemoModel &lhs, const DemoModel &rhs) {
        return &lhs == &rhs;
    };
    bool operator!=(const DemoModel &lhs, const DemoModel &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    
    //type name trait
    
    //static type name trait
    
    //polymorphic to string
    std::string DemoModel::toString() const
    {
        std::string res = "DemoModel\n";
        res += "\tboolean_property = " + rd::to_string(boolean_property_) + '\n';
        res += "\tboolean_array = " + rd::to_string(boolean_array_) + '\n';
        res += "\tscalar = " + rd::to_string(scalar_) + '\n';
        res += "\tubyte = " + rd::to_string(ubyte_) + '\n';
        res += "\tubyte_array = " + rd::to_string(ubyte_array_) + '\n';
        res += "\tlist = " + rd::to_string(list_) + '\n';
        res += "\tset = " + rd::to_string(set_) + '\n';
        res += "\tmapLongToString = " + rd::to_string(mapLongToString_) + '\n';
        res += "\tcall = " + rd::to_string(call_) + '\n';
        res += "\tcallback = " + rd::to_string(callback_) + '\n';
        res += "\tinterned_string = " + rd::to_string(interned_string_) + '\n';
        res += "\tpolymorphic = " + rd::to_string(polymorphic_) + '\n';
        res += "\tenum = " + rd::to_string(enum_) + '\n';
        return res;
    }
    
    //external to string
    std::string to_string(const DemoModel & value)
    {
        return value.toString();
    }
};
