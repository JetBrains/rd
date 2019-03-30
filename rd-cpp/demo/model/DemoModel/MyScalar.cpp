#include "MyScalar.h"


namespace demo {
    
    //companion
    
    //initializer
    void MyScalar::initialize()
    {
    }
    
    //primary ctor
    MyScalar::MyScalar(bool sign_, signed char byte_, short short_, int32_t int_, int64_t long_) :
    rd::IPolymorphicSerializable()
    ,sign_(std::move(sign_)), byte_(std::move(byte_)), short_(std::move(short_)), int_(std::move(int_)), long_(std::move(long_))
    {
        initialize();
    }
    
    //secondary constructor
    
    //default ctors and dtors
    MyScalar::MyScalar()
    {
        initialize();
    }
    
    //reader
    MyScalar MyScalar::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
    {
        auto sign_ = buffer.readBool();
        auto byte_ = buffer.read_integral<signed char>();
        auto short_ = buffer.read_integral<short>();
        auto int_ = buffer.read_integral<int32_t>();
        auto long_ = buffer.read_integral<int64_t>();
        MyScalar res{std::move(sign_), std::move(byte_), std::move(short_), std::move(int_), std::move(long_)};
        return res;
    }
    
    //writer
    void MyScalar::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
    {
        buffer.writeBool(sign_);
        buffer.write_integral(byte_);
        buffer.write_integral(short_);
        buffer.write_integral(int_);
        buffer.write_integral(long_);
    }
    
    //virtual init
    
    //identify
    
    //getters
    bool const & MyScalar::get_sign() const
    {
        return sign_;
    }
    signed char const & MyScalar::get_byte() const
    {
        return byte_;
    }
    short const & MyScalar::get_short() const
    {
        return short_;
    }
    int32_t const & MyScalar::get_int() const
    {
        return int_;
    }
    int64_t const & MyScalar::get_long() const
    {
        return long_;
    }
    
    //intern
    
    //equals trait
    bool MyScalar::equals(rd::ISerializable const& object) const
    {
        auto const &other = dynamic_cast<MyScalar const&>(object);
        if (this == &other) return true;
        if (this->sign_ != other.sign_) return false;
        if (this->byte_ != other.byte_) return false;
        if (this->short_ != other.short_) return false;
        if (this->int_ != other.int_) return false;
        if (this->long_ != other.long_) return false;
        
        return true;
    }
    
    //equality operators
    bool operator==(const MyScalar &lhs, const MyScalar &rhs){
        if (lhs.type_name() != rhs.type_name()) return false;
        return lhs.equals(rhs);
    }
    bool operator!=(const MyScalar &lhs, const MyScalar &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    size_t MyScalar::hashCode() const
    {
        size_t __r = 0;
        __r = __r * 31 + (std::hash<bool>()(get_sign()));
        __r = __r * 31 + (std::hash<signed char>()(get_byte()));
        __r = __r * 31 + (std::hash<short>()(get_short()));
        __r = __r * 31 + (std::hash<int32_t>()(get_int()));
        __r = __r * 31 + (std::hash<int64_t>()(get_long()));
        return __r;
    }
    std::string MyScalar::type_name() const
    {
        return "MyScalar";
    }
};
