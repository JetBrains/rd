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
        return res;
    }
    
    //writer
    
    //virtual init
    
    //identify
    
    //getters
    
    //intern
    
    //equals trait
    
    //equality operators
    bool operator==(const Base &lhs, const Base &rhs){
        return &lhs == &rhs;
    }
    bool operator!=(const Base &lhs, const Base &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    size_t Base::hashCode() const
    {
        size_t __r = 0;
        return __r;
    }
    std::string Base::type_name() const
    {
        return "Base";
    }
};
