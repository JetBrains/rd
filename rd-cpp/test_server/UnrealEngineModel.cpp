#include "UnrealEngineModel.h"


//companion

UnrealEngineModel::UnrealEngineModelSerializersOwner UnrealEngineModel::serializersOwner;

void UnrealEngineModel::UnrealEngineModelSerializersOwner::registerSerializersCore(Serializers const& serializers)
{
}

UnrealEngineModel UnrealEngineModel::create(Lifetime lifetime, IProtocol * protocol) {
        UnrealEngineModel::serializersOwner.registry(protocol->serializers);
        
        UnrealEngineModel res;
        res.identify(*(protocol->identity), RdId::Null().mix("UnrealEngineModel"));
        res.bind(lifetime, protocol, "UnrealEngineModel");
        return res;
}


//initializer
void UnrealEngineModel::init()
{
        _test_connection.optimizeNested = true;
        _filename_to_open.optimizeNested = true;
        bindableChildren.emplace_back("test_connection", &_test_connection);
        bindableChildren.emplace_back("filename_to_open", &_filename_to_open);
        serializationHash = 6166018688604674907L;
}

//primary ctor
UnrealEngineModel::UnrealEngineModel(RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> _test_connection, RdProperty<tl::optional<std::wstring>, UnrealEngineModel::__StringNullableSerializer> _filename_to_open): RdExtBase(), _test_connection(std::move(_test_connection)), _filename_to_open(std::move(_filename_to_open)) { init(); }
//reader

//writer

//getters
RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> const & UnrealEngineModel::get_test_connection() const  { return _test_connection; }
RdProperty<tl::optional<std::wstring>, UnrealEngineModel::__StringNullableSerializer> const & UnrealEngineModel::get_filename_to_open() const  { return _filename_to_open; }

//equals trait

//hash code trait
