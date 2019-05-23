#include "MyScalar.h"


namespace demo {
    
    //companion
    
    //initializer
    void MyScalar::initialize()
    {
    }
    
    //primary ctor
    MyScalar::MyScalar(bool bool_, uint8_t byte_, int16_t short_, int32_t int_, int64_t long_, float float_, double double_, uint8_t unsigned_byte_, uint16_t unsigned_short_, uint32_t unsigned_int_, uint64_t unsigned_long_) :
    rd::IPolymorphicSerializable()
    ,bool_(std::move(bool_)), byte_(std::move(byte_)), short_(std::move(short_)), int_(std::move(int_)), long_(std::move(long_)), float_(std::move(float_)), double_(std::move(double_)), unsigned_byte_(std::move(unsigned_byte_)), unsigned_short_(std::move(unsigned_short_)), unsigned_int_(std::move(unsigned_int_)), unsigned_long_(std::move(unsigned_long_))
    {
        initialize();
    }
    
    //secondary constructor
    
    //default ctors and dtors
    
    //reader
    MyScalar MyScalar::read(rd::SerializationCtx& ctx, rd::Buffer & buffer)
    {
        auto bool_ = buffer.read_bool();
        auto byte_ = buffer.read_integral<uint8_t>();
        auto short_ = buffer.read_integral<int16_t>();
        auto int_ = buffer.read_integral<int32_t>();
        auto long_ = buffer.read_integral<int64_t>();
        auto float_ = buffer.read_floating_point<float>();
        auto double_ = buffer.read_floating_point<double>();
        auto unsigned_byte_ = buffer.read_integral<uint8_t>();
        auto unsigned_short_ = buffer.read_integral<uint16_t>();
        auto unsigned_int_ = buffer.read_integral<uint32_t>();
        auto unsigned_long_ = buffer.read_integral<uint64_t>();
        MyScalar res{std::move(bool_), std::move(byte_), std::move(short_), std::move(int_), std::move(long_), std::move(float_), std::move(double_), std::move(unsigned_byte_), std::move(unsigned_short_), std::move(unsigned_int_), std::move(unsigned_long_)};
        return res;
    }
    
    //writer
    void MyScalar::write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const
    {
        buffer.write_bool(bool_);
        buffer.write_integral(byte_);
        buffer.write_integral(short_);
        buffer.write_integral(int_);
        buffer.write_integral(long_);
        buffer.write_floating_point(float_);
        buffer.write_floating_point(double_);
        buffer.write_integral(unsigned_byte_);
        buffer.write_integral(unsigned_short_);
        buffer.write_integral(unsigned_int_);
        buffer.write_integral(unsigned_long_);
    }
    
    //virtual init
    
    //identify
    
    //getters
    bool const & MyScalar::get_bool() const
    {
        return bool_;
    }
    uint8_t const & MyScalar::get_byte() const
    {
        return byte_;
    }
    int16_t const & MyScalar::get_short() const
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
    float const & MyScalar::get_float() const
    {
        return float_;
    }
    double const & MyScalar::get_double() const
    {
        return double_;
    }
    uint8_t const & MyScalar::get_unsigned_byte() const
    {
        return unsigned_byte_;
    }
    uint16_t const & MyScalar::get_unsigned_short() const
    {
        return unsigned_short_;
    }
    uint32_t const & MyScalar::get_unsigned_int() const
    {
        return unsigned_int_;
    }
    uint64_t const & MyScalar::get_unsigned_long() const
    {
        return unsigned_long_;
    }
    
    //intern
    
    //equals trait
    bool MyScalar::equals(rd::ISerializable const& object) const
    {
        auto const &other = dynamic_cast<MyScalar const&>(object);
        if (this == &other) return true;
        if (this->bool_ != other.bool_) return false;
        if (this->byte_ != other.byte_) return false;
        if (this->short_ != other.short_) return false;
        if (this->int_ != other.int_) return false;
        if (this->long_ != other.long_) return false;
        if (this->float_ != other.float_) return false;
        if (this->double_ != other.double_) return false;
        if (this->unsigned_byte_ != other.unsigned_byte_) return false;
        if (this->unsigned_short_ != other.unsigned_short_) return false;
        if (this->unsigned_int_ != other.unsigned_int_) return false;
        if (this->unsigned_long_ != other.unsigned_long_) return false;
        
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
        __r = __r * 31 + (std::hash<bool>()(get_bool()));
        __r = __r * 31 + (std::hash<uint8_t>()(get_byte()));
        __r = __r * 31 + (std::hash<int16_t>()(get_short()));
        __r = __r * 31 + (std::hash<int32_t>()(get_int()));
        __r = __r * 31 + (std::hash<int64_t>()(get_long()));
        __r = __r * 31 + (std::hash<float>()(get_float()));
        __r = __r * 31 + (std::hash<double>()(get_double()));
        __r = __r * 31 + (std::hash<uint8_t>()(get_unsigned_byte()));
        __r = __r * 31 + (std::hash<uint16_t>()(get_unsigned_short()));
        __r = __r * 31 + (std::hash<uint32_t>()(get_unsigned_int()));
        __r = __r * 31 + (std::hash<uint64_t>()(get_unsigned_long()));
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
        res += "\tbool = " + rd::to_string(bool_) + '\n';
        res += "\tbyte = " + rd::to_string(byte_) + '\n';
        res += "\tshort = " + rd::to_string(short_) + '\n';
        res += "\tint = " + rd::to_string(int_) + '\n';
        res += "\tlong = " + rd::to_string(long_) + '\n';
        res += "\tfloat = " + rd::to_string(float_) + '\n';
        res += "\tdouble = " + rd::to_string(double_) + '\n';
        res += "\tunsigned_byte = " + rd::to_string(unsigned_byte_) + '\n';
        res += "\tunsigned_short = " + rd::to_string(unsigned_short_) + '\n';
        res += "\tunsigned_int = " + rd::to_string(unsigned_int_) + '\n';
        res += "\tunsigned_long = " + rd::to_string(unsigned_long_) + '\n';
        return res;
    }
    
    //external to string
    std::string to_string(const MyScalar & value)
    {
        return value.toString();
    }
};
