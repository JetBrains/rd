#include "UnrealEngineModel.h"


//companion

void UnrealEngineModel::UnrealEngineModelSerializersOwner::registerSerializersCore(Serializers const& serializers)
{
        UnrealEngineModel::serializersOwner.registry(serializers);
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
        _test_string.optimizeNested = true;
        bindableChildren.emplace_back("test_connection", &_test_connection);
        bindableChildren.emplace_back("test_string", &_test_string);
        serializationHash = 5138970996713995165L;
}

//primary ctor
UnrealEngineModel::UnrealEngineModel(RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> _test_connection, RdProperty<tl::optional<std::string>, UnrealEngineModel::__StringNullableSerializer> _test_string): RdExtBase(), _test_connection(std::move(_test_connection)), _test_string(std::move(_test_string)) { init(); }
//reader

//writer

//getters
RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> const & UnrealEngineModel::get_test_connection() const  { return _test_connection; }
RdProperty<tl::optional<std::string>, UnrealEngineModel::__StringNullableSerializer> const & UnrealEngineModel::get_test_string() const  { return _test_string; }

//equals trait

//hash code trait
