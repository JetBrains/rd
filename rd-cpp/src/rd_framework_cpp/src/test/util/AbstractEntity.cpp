#include "AbstractEntity.h"


namespace rd {
	namespace test {
		namespace util {
			//companion

			//initializer
			void AbstractEntity::initialize()
			{
			}

			//primary ctor
			AbstractEntity::AbstractEntity(::std::wstring filePath): ISerializable(), filePath(::std::move(filePath)) { initialize(); }
			//reader
			Wrapper<AbstractEntity> AbstractEntity::readUnknownInstance(SerializationCtx const& ctx, Buffer const & buffer, RdId const& unknownId, int32_t size)
			{
				return Wrapper<AbstractEntity>();
			}

			//writer

			//virtual init

			//identify

			//getters
			::std::wstring const & AbstractEntity::get_filePath() const
			{
				return filePath;
			}

			//equals trait
			bool AbstractEntity::equals(ISerializable const& object) const
			{
				auto const &other = dynamic_cast<AbstractEntity const&>(object);
				return this == &other;
			}

			//equality operators
			bool operator==(const AbstractEntity &lhs, const AbstractEntity &rhs){
				return &lhs == &rhs;
			}
			bool operator!=(const AbstractEntity &lhs, const AbstractEntity &rhs){
				return !(lhs == rhs);
			}

			//hash code trait
			size_t AbstractEntity::hashCode() const
			{
				size_t __r = 0;
				__r = __r * 31 + (::std::hash<::std::wstring>()(get_filePath()));
				return __r;
			}
	
		}
	}
}
