#include "Derived.h"


namespace demo {
    
    //companion
    
    //initializer
    void Derived::initialize()
    {
    }
    
    //primary ctor
    Derived::Derived(rd::Wrapper<std::wstring> string_) :
    Base()
    ,string_(std::move(string_))
    {
        initialize();
    }
    
    //secondary constructor
    
    //default ctors and dtors
    Derived::Derived()
    {
        initialize();
    }
    
    //reader
    Derived Derived::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
    {
        auto string_ = buffer.readWString();
        Derived res{std::move(string_)};
        return res;
    }
    
    //writer
    void Derived::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
    {
        buffer.writeWString(string_);
    }
    
    //virtual init
    
    //identify
    
    //getters
    std::wstring const & Derived::get_string() const
    {
        return *string_;
    }
    
    //intern
    
    //equals trait
    bool Derived::equals(rd::ISerializable const& object) const
    {
        auto const &other = dynamic_cast<Derived const&>(object);
        if (this == &other) return true;
        if (this->string_ != other.string_) return false;
        
        return true;
    }
    
    //equality operators
    bool operator==(const Derived &lhs, const Derived &rhs){
        if (lhs.type_name() != rhs.type_name()) return false;
        return lhs.equals(rhs);
    }
    bool operator!=(const Derived &lhs, const Derived &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    size_t Derived::hashCode() const
    {
        size_t __r = 0;
        __r = __r * 31 + (std::hash<std::wstring>()(get_string()));
        return __r;
    }
    std::string Derived::type_name() const
    {
        return "Derived";
    }
};
