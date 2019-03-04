//
// Created by jetbrains on 07.08.2018.
//

#ifndef RD_CPP_ISERIALIZABLE_H
#define RD_CPP_ISERIALIZABLE_H

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
	};
}


#endif //RD_CPP_ISERIALIZABLE_H
