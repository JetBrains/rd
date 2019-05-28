#ifndef DemoModel_H
#define DemoModel_H

#include "Buffer.h"
#include "Identities.h"
#include "Protocol.h"
#include "RdId.h"
#include "RdSignal.h"
#include "RdProperty.h"
#include "RdList.h"
#include "RdSet.h"
#include "RdMap.h"
#include "ISerializable.h"
#include "ISerializersOwner.h"
#include "IUnknownInstance.h"
#include "Polymorphic.h"
#include "NullableSerializer.h"
#include "ArraySerializer.h"
#include "InternedSerializer.h"
#include "SerializationCtx.h"
#include "Serializers.h"
#include "RdExtBase.h"
#include "RdCall.h"
#include "RdEndpoint.h"
#include "RdTask.h"
#include "gen_util.h"

#include <cstring>
#include <cstdint>
#include <vector>

#include "thirdparty.hpp"
#include "instantiations.h"

#include "MyScalar.h"
#include "Base.h"
#include "MyEnum.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
#pragma warning( disable:4267 )
#pragma warning( disable:4244 )
namespace demo {
    class DemoModel : public rd::RdExtBase
    {
        
        //companion
        
        public:
        struct DemoModelSerializersOwner : public rd::ISerializersOwner {
            void registerSerializersCore(rd::Serializers const& serializers) const override;
        };
        
        static const DemoModelSerializersOwner serializersOwner;
        
        
        public:
        void connect(rd::Lifetime lifetime, rd::IProtocol const * protocol);
        
        
        //custom serializers
        private:
        using __BoolArraySerializer = rd::ArraySerializer<rd::Polymorphic<bool>>;
        using __UByteArraySerializer = rd::ArraySerializer<rd::Polymorphic<uint8_t>>;
        using __StringInternedAtProtocolSerializer = rd::InternedSerializer<rd::Polymorphic<std::wstring>, rd::util::getPlatformIndependentHash("Protocol")>;
        
        //constants
        public:
        static constexpr bool const_toplevel = true;
        
        //fields
        protected:
        rd::RdProperty<bool, rd::Polymorphic<bool>> boolean_property_;
        rd::RdProperty<std::vector<bool>, DemoModel::__BoolArraySerializer> boolean_array_;
        rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> scalar_;
        rd::RdProperty<uint8_t, rd::Polymorphic<uint8_t>> ubyte_;
        rd::RdProperty<std::vector<uint8_t>, DemoModel::__UByteArraySerializer> ubyte_array_;
        rd::RdList<int32_t, rd::Polymorphic<int32_t>> list_;
        rd::RdSet<int32_t, rd::Polymorphic<int32_t>> set_;
        rd::RdMap<int64_t, std::wstring, rd::Polymorphic<int64_t>, rd::Polymorphic<std::wstring>> mapLongToString_;
        rd::RdCall<wchar_t, std::wstring, rd::Polymorphic<wchar_t>, rd::Polymorphic<std::wstring>> call_;
        rd::RdEndpoint<std::wstring, int32_t, rd::Polymorphic<std::wstring>, rd::Polymorphic<int32_t>> callback_;
        rd::RdProperty<std::wstring, DemoModel::__StringInternedAtProtocolSerializer> interned_string_;
        rd::RdProperty<Base, rd::AbstractPolymorphic<Base>> polymorphic_;
        rd::RdProperty<MyEnum, rd::Polymorphic<MyEnum>> enum_;
        
        
        //initializer
        private:
        void initialize();
        
        //primary ctor
        public:
        DemoModel(rd::RdProperty<bool, rd::Polymorphic<bool>> boolean_property_, rd::RdProperty<std::vector<bool>, DemoModel::__BoolArraySerializer> boolean_array_, rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> scalar_, rd::RdProperty<uint8_t, rd::Polymorphic<uint8_t>> ubyte_, rd::RdProperty<std::vector<uint8_t>, DemoModel::__UByteArraySerializer> ubyte_array_, rd::RdList<int32_t, rd::Polymorphic<int32_t>> list_, rd::RdSet<int32_t, rd::Polymorphic<int32_t>> set_, rd::RdMap<int64_t, std::wstring, rd::Polymorphic<int64_t>, rd::Polymorphic<std::wstring>> mapLongToString_, rd::RdCall<wchar_t, std::wstring, rd::Polymorphic<wchar_t>, rd::Polymorphic<std::wstring>> call_, rd::RdEndpoint<std::wstring, int32_t, rd::Polymorphic<std::wstring>, rd::Polymorphic<int32_t>> callback_, rd::RdProperty<std::wstring, DemoModel::__StringInternedAtProtocolSerializer> interned_string_, rd::RdProperty<Base, rd::AbstractPolymorphic<Base>> polymorphic_, rd::RdProperty<MyEnum, rd::Polymorphic<MyEnum>> enum_);
        
        //secondary constructor
        
        //default ctors and dtors
        
        DemoModel();
        
        DemoModel(DemoModel &&) = delete;
        
        DemoModel& operator=(DemoModel &&) = delete;
        
        virtual ~DemoModel() = default;
        
        //reader
        
        //writer
        
        //virtual init
        void init(rd::Lifetime lifetime) const override;
        
        //identify
        void identify(const rd::Identities &identities, rd::RdId const &id) const override;
        
        //getters
        rd::RdProperty<bool, rd::Polymorphic<bool>> const & get_boolean_property() const;
        rd::RdProperty<std::vector<bool>, DemoModel::__BoolArraySerializer> const & get_boolean_array() const;
        rd::RdProperty<MyScalar, rd::Polymorphic<MyScalar>> const & get_scalar() const;
        rd::RdProperty<uint8_t, rd::Polymorphic<uint8_t>> const & get_ubyte() const;
        rd::RdProperty<std::vector<uint8_t>, DemoModel::__UByteArraySerializer> const & get_ubyte_array() const;
        rd::RdList<int32_t, rd::Polymorphic<int32_t>> const & get_list() const;
        rd::RdSet<int32_t, rd::Polymorphic<int32_t>> const & get_set() const;
        rd::RdMap<int64_t, std::wstring, rd::Polymorphic<int64_t>, rd::Polymorphic<std::wstring>> const & get_mapLongToString() const;
        rd::RdCall<wchar_t, std::wstring, rd::Polymorphic<wchar_t>, rd::Polymorphic<std::wstring>> const & get_call() const;
        rd::RdEndpoint<std::wstring, int32_t, rd::Polymorphic<std::wstring>, rd::Polymorphic<int32_t>> const & get_callback() const;
        rd::RdProperty<std::wstring, DemoModel::__StringInternedAtProtocolSerializer> const & get_interned_string() const;
        rd::RdProperty<Base, rd::AbstractPolymorphic<Base>> const & get_polymorphic() const;
        rd::RdProperty<MyEnum, rd::Polymorphic<MyEnum>> const & get_enum() const;
        
        //intern
        
        //equals trait
        private:
        
        //equality operators
        public:
        friend bool operator==(const DemoModel &lhs, const DemoModel &rhs);
        friend bool operator!=(const DemoModel &lhs, const DemoModel &rhs);
        
        //hash code trait
        
        //type name trait
        
        //static type name trait
        
        //polymorphic to string
        private:
        std::string toString() const override;
        
        //external to string
        public:
        friend std::string to_string(const DemoModel & value);
    };
};

#pragma warning( pop )


//hash code trait

#endif // DemoModel_H
