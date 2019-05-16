#ifndef RD_CPP_POLYMORPHIC_H
#define RD_CPP_POLYMORPHIC_H

#include "Buffer.h"
#include "RdReactiveBase.h"

#include <type_traits>


namespace rd {
	//region predeclared

	class SerializationCtx;
	//endregion

	template<typename T, typename R = void>
	class Polymorphic {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
			return T::read(ctx, buffer);
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			value.write(ctx, buffer);
		}
		
		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper<T> const &value) {
			value->write(ctx, buffer);
		}
	};


	template<typename T>
	class Polymorphic<T, typename std::enable_if_t<std::is_integral<T>::value>> {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_integral<T>();
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			buffer.write_integral<T>(value);
		}
	};

	template<typename T>
	class Polymorphic<T, typename std::enable_if_t<std::is_floating_point<T>::value>> {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_floating_point<T>();
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			buffer.write_floating_point<T>(value);
		}
	};

	//class Polymorphic<int, void>;

	template<typename T>
	class Polymorphic<std::vector<T>> {
	public:
		static std::vector<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_array<T>();
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, std::vector<T> const &value) {
			buffer.write_array<T>(value);
		}
	};

	template<>
	class Polymorphic<bool> {
	public:
		static bool read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_bool();
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, bool const &value) {
			buffer.write_bool(value);
		}
	};

	template<>
	class Polymorphic<std::wstring> {
	public:
		static std::wstring read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_wstring();
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, std::wstring const &value) {
			buffer.write_wstring(value);
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper<std::wstring> const &value) {
			buffer.write_wstring(*value);
		}
	};

	template<>
	class Polymorphic<Void> {
	public:
		static Void read(SerializationCtx const &ctx, Buffer const &buffer) {
			return {};
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, Void const &value) {}
	};

	template<typename T>
	class Polymorphic<T, typename std::enable_if_t<util::is_base_of_v<RdReactiveBase, T>>> {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
			return T::read(ctx, buffer);
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			value.write(ctx, buffer);
		}
	};

	template<typename T>
	class Polymorphic<T, typename std::enable_if_t<std::is_enum<T>::value>> {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_enum<T>();
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			buffer.write_enum<T>(value);
		}
	};

	template<typename T>
	class Polymorphic<optional<T>> {
	public:
		static optional<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.read_nullable<T>([&ctx, &buffer]() {
				return Polymorphic<T>::read(ctx, buffer);
			});
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, optional<T> const &value) {
			buffer.write_nullable<T>(value, [&ctx, &buffer](T const &v) {
				Polymorphic<T>::write(ctx, buffer, v);
			});
		}
	};

	template<typename T>
	class Polymorphic<Wrapper<T>> {
	public:
		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper<T> const &value) {
			value->write(ctx, buffer);
		}
	};
}

extern template class rd::Polymorphic<int8_t>;
extern template class rd::Polymorphic<int16_t>;
extern template class rd::Polymorphic<int32_t>;
extern template class rd::Polymorphic<int64_t>;

#endif //RD_CPP_POLYMORPHIC_H
