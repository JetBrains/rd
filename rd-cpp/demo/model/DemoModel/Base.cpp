#include "Base.h"


#include "Base_Unknown.h"
namespace demo {
    
    //companion
    
    //initializer
    void Base::initialize()
    {
    }
    
    //primary ctor
    
    //secondary constructor
    
    //default ctors and dtors
    Base::Base()
    {
        initialize();
    }
    
    //reader
    rd::Wrapper<Base> Base::readUnknownInstance(rd::SerializationCtx const& ctx, rd::Buffer const & buffer, rd::RdId const& unknownId, int32_t size)
    {
        int32_t objectStartPosition = buffer.get_position();
        auto unknownBytes = rd::Buffer::ByteArray(objectStartPosition + size - buffer.get_position());
        buffer.readByteArrayRaw(unknownBytes);
        Base_Unknown res{unknownId, unknownBytes};
        return rd::Wrapper<Base_Unknown>(std::move(res));
    }
    
    //writer
    
    //virtual init
    
    //identify
    
    //getters
    
    //intern
    
    //equals trait
    
    //equality operators
    bool operator==(const Base &lhs, const Base &rhs) {
        if (lhs.type_name() != rhs.type_name()) return false;
        return lhs.equals(rhs);
    };
    bool operator!=(const Base &lhs, const Base &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    size_t Base::hashCode() const
    {
        size_t __r = 0;
        return __r;
    }
    
    //type name trait
    std::string Base::type_name() const
    {
        return "Base";
    }
    
    //static type name trait
    std::string Base::static_type_name()
    {
        return "Base";
    }
    
    //to string trait
    std::string to_string(const Base & value)
    {
        std::string res = "Base\n";
        return res;
    }
};
