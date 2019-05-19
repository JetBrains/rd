#include "MyScalar.h"


namespace demo {
    
    //companion
    
    //initializer
    void MyScalar::initialize()
    {
    }
    
    //primary ctor
    MyScalar::MyScalar(bool sign_, signed char byte_, short short_, int32_t int_, int64_t long_, float float__, double double__) :
    rd::IPolymorphicSerializable()
    ,sign_(std::move(sign_)), byte_(std::move(byte_)), short_(std::move(short_)), int_(std::move(int_)), long_(std::move(long_)), float__(std::move(float__)), double__(std::move(double__))
    {
        initialize();
    }
    
    //secondary constructor
    
    //default ctors and dtors
    
    //reader
    MyScalar MyScalar::read(rd::SerializationCtx& ctx, rd::Buffer & buffer)
    {
        auto sign_ = buffer.read_bool();
        auto byte_ = buffer.read_integral<signed char>();
        auto short_ = buffer.read_integral<short>();
        auto int_ = buffer.read_integral<int32_t>();
        auto long_ = buffer.read_integral<int64_t>();
        auto float__ = buffer.read_floating_point<float>();
        auto double__ = buffer.read_floating_point<double>();
        MyScalar res{std::move(sign_), std::move(byte_), std::move(short_), std::move(int_), std::move(long_), std::move(float__), std::move(double__)};
        return res;
    }
    
    //writer
    void MyScalar::write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const
    {
        buffer.write_bool(sign_);
        buffer.write_integral(byte_);
        buffer.write_integral(short_);
        buffer.write_integral(int_);
        buffer.write_integral(long_);
        buffer.write_floating_point(float__);
        buffer.write_floating_point(double__);
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
    float const & MyScalar::get_float_() const
    {
        return float__;
    }
    double const & MyScalar::get_double_() const
    {
        return double__;
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
        if (this->float__ != other.float__) return false;
        if (this->double__ != other.double__) return false;
        
        return true;
    }
    
    //equality operators
    bool operator==(const MyScalar &lhs, const MyScalar &rhs) {
        if (lhs.type_name() != rhs.type_name()) return false;
        return lhs.equals(rhs);
    };
    bool operator!=(const MyScalar &lhs, const MyScalar &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    size_t MyScalar::hashCode() const noexcept
    {
        size_t __r = 0;
        __r = __r * 31 + (std::hash<bool>()(get_sign()));
        __r = __r * 31 + (std::hash<signed char>()(get_byte()));
        __r = __r * 31 + (std::hash<short>()(get_short()));
        __r = __r * 31 + (std::hash<int32_t>()(get_int()));
        __r = __r * 31 + (std::hash<int64_t>()(get_long()));
        __r = __r * 31 + (std::hash<float>()(get_float_()));
        __r = __r * 31 + (std::hash<double>()(get_double_()));
        return __r;
    }
    
    //type name trait
    std::string MyScalar::type_name() const
    {
        return "MyScalar";
    }
    
    //static type name trait
    std::string MyScalar::static_type_name()
    {
        return "MyScalar";
    }
    
    //polymorphic to string
    std::string MyScalar::toString() const
    {
        std::string res = "MyScalar\n";
        res += "\tsign = " + rd::to_string(sign_) + '\n';
        res += "\tbyte = " + rd::to_string(byte_) + '\n';
        res += "\tshort = " + rd::to_string(short_) + '\n';
        res += "\tint = " + rd::to_string(int_) + '\n';
        res += "\tlong = " + rd::to_string(long_) + '\n';
        res += "\tfloat_ = " + rd::to_string(float__) + '\n';
        res += "\tdouble_ = " + rd::to_string(double__) + '\n';
        return res;
    }
    
    //external to string
    std::string to_string(const MyScalar & value)
    {
        return value.toString();
    }
};
