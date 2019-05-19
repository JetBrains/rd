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
    
    //reader
    Derived Derived::read(rd::SerializationCtx& ctx, rd::Buffer & buffer)
    {
        auto string_ = buffer.read_wstring();
        Derived res{std::move(string_)};
        return res;
    }
    
    //writer
    void Derived::write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const
    {
        buffer.write_wstring(string_);
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
    bool operator==(const Derived &lhs, const Derived &rhs) {
        if (lhs.type_name() != rhs.type_name()) return false;
        return lhs.equals(rhs);
    };
    bool operator!=(const Derived &lhs, const Derived &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    size_t Derived::hashCode() const noexcept
    {
        size_t __r = 0;
        __r = __r * 31 + (std::hash<std::wstring>()(get_string()));
        return __r;
    }
    
    //type name trait
    std::string Derived::type_name() const
    {
        return "Derived";
    }
    
    //static type name trait
    std::string Derived::static_type_name()
    {
        return "Derived";
    }
    
    //polymorphic to string
    std::string Derived::toString() const
    {
        std::string res = "Derived\n";
        res += "\tstring = " + rd::to_string(string_) + '\n';
        return res;
    }
    
    //external to string
    std::string to_string(const Derived & value)
    {
        return value.toString();
    }
};
