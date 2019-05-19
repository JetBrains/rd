#ifndef RD_CPP_ISERIALIZABLE_H
#define RD_CPP_ISERIALIZABLE_H

#include <string>

namespace rd {
	//region predeclared

	class Buffer;

	class SerializationCtx;
	//endregion

	/**
	 * \brief Provides \ref write for serialization to be overriden. For deserialization derived class must have static
	 * method read. See examples for more information.
	 */
	class ISerializable {
	public:
		virtual ~ISerializable() = default;

		virtual void write(SerializationCtx  &ctx, Buffer &buffer) const = 0;
	};

	/**
	 * \brief Dynamically polymorhic node.
	 */
	class IPolymorphicSerializable : public ISerializable {
	public:
		/**
		 * \return actual class's name as written in source code.
		 */
		virtual std::string type_name() const = 0/*{ throw std::invalid_argument("type doesn't support polymorphic serialization"); }*/;

//		virtual bool equals(IPolymorphicSerializable const& object) const = 0;

		virtual size_t hashCode() const noexcept;

		virtual std::string toString() const = 0;

		virtual bool equals(ISerializable const &) const = 0;

		friend bool operator==(const IPolymorphicSerializable &lhs, const IPolymorphicSerializable &rhs);

		friend bool operator!=(const IPolymorphicSerializable &lhs, const IPolymorphicSerializable &rhs);
	};
}

namespace std {
	template <> struct hash<rd::IPolymorphicSerializable> {
		size_t operator()(const rd::IPolymorphicSerializable & value) const noexcept {
			return value.hashCode();
		}
	};
}


#endif //RD_CPP_ISERIALIZABLE_H
