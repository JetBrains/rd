//
// Created by jetbrains on 07.08.2018.
//

#ifndef RD_CPP_ISERIALIZABLE_H
#define RD_CPP_ISERIALIZABLE_H

#include <string>

namespace rd {
	//region predeclared

	class Buffer;
	//endregion

	class SerializationCtx;

	class ISerializable {
	public:
		virtual ~ISerializable() = default;

		virtual void write(SerializationCtx const &ctx, Buffer const &buffer) const = 0;
	};

	class IPolymorphicSerializable : public ISerializable {
	public:
		virtual std::string type_name() const = 0/*{ throw std::invalid_argument("type doesn't support polymorphic serialization"); }*/;

//		virtual bool equals(IPolymorphicSerializable const& object) const = 0;

		virtual size_t hashCode() const {
			return std::hash<void const *>()(static_cast<void const *>(this));
		}

		virtual bool equals(ISerializable const &) const = 0;

		friend bool operator==(const IPolymorphicSerializable &lhs, const IPolymorphicSerializable &rhs) {
			return lhs.equals(rhs);
		}

		friend bool operator!=(const IPolymorphicSerializable &lhs, const IPolymorphicSerializable &rhs) {
			return !(lhs == rhs);
		}
	};
}

namespace std {
	template <> struct hash<rd::IPolymorphicSerializable> {
		size_t operator()(const rd::IPolymorphicSerializable & value) const {
			return value.hashCode();
		}
	};
}


#endif //RD_CPP_ISERIALIZABLE_H
