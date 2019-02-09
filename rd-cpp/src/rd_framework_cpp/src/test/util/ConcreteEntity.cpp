#include "ConcreteEntity.h"



//companion

//initializer
void ConcreteEntity::initialize()
{
}

//primary ctor
ConcreteEntity::ConcreteEntity(std::wstring filePath): AbstractEntity(std::move(filePath)) { initialize(); }
//reader
ConcreteEntity ConcreteEntity::read(SerializationCtx const& ctx, Buffer const & buffer)
{
    auto filePath = buffer.readWString();
    ConcreteEntity res{std::move(filePath)};
    return res;
}

//writer
void ConcreteEntity::write(SerializationCtx const& ctx, Buffer const& buffer) const
{
    buffer.writeWString(filePath);
}

//virtual init

//identify

//getters

//equals trait
bool ConcreteEntity::equals(ISerializable const& object) const
{
    auto const &other = dynamic_cast<ConcreteEntity const&>(object);
    if (this == &other) return true;
    if (this->filePath != other.filePath) return false;
    
    return true;
}

//equality operators
bool operator==(const ConcreteEntity &lhs, const ConcreteEntity &rhs){
    if (typeid(lhs) != typeid(rhs)) return false;
    return lhs.equals(rhs);
}
bool operator!=(const ConcreteEntity &lhs, const ConcreteEntity &rhs){
    return !(lhs == rhs);
}

//hash code trait
size_t ConcreteEntity::hashCode() const
{
    size_t __r = 0;
    __r = __r * 31 + (std::hash<std::wstring>()(get_filePath()));
    return __r;
}
