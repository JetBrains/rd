//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_UNSAFEBUFFER_H
#define RD_CPP_UNSAFEBUFFER_H

#include "core_util.h"
#include "wrapper.h"

#include "optional.hpp"

#include <vector>
#include <type_traits>
#include <functional>
#include <memory>

namespace rd {
	class Buffer {
	public:
		using word_t = uint8_t;

		using ByteArray = std::vector<word_t>;
	protected:

		mutable ByteArray data_;
		mutable size_t offset = 0;

		void require_available(size_t size) const;

		//read
		void read(word_t *dst, size_t size) const;

		//write
		void write(const word_t *src, size_t size) const;

	public:
		//region ctor/dtor

		explicit Buffer(size_t initialSize = 10); //todo

		explicit Buffer(const ByteArray &array, size_t offset = 0);

		explicit Buffer(ByteArray &&array, size_t offset = 0);

		Buffer(Buffer const &) = delete;

		Buffer(Buffer &&) = default;
		//endregion

		size_t get_position() const;

		void set_position(size_t value) const;

		void check_available(size_t moreSize) const;

		void rewind() const;

		template<typename T, typename = typename std::enable_if_t<std::is_integral<T>::value, T>>
		T read_integral() const {
			T result;
			read(reinterpret_cast<word_t *>(&result), sizeof(T));
			return result;
		}

		template<typename T, typename = typename std::enable_if_t<std::is_integral<T>::value>>
		void write_integral(T const &value) const {
			write(reinterpret_cast<word_t const *>(&value), sizeof(T));
		}

		template<typename T, typename = typename std::enable_if_t<std::is_floating_point<T>::value, T>>
		T read_floating_point() const {
			T result;
			read(reinterpret_cast<word_t *>(&result), sizeof(T));
			return result;
			//todo check correctness
		}

		template<typename T, typename = typename std::enable_if_t<std::is_floating_point<T>::value>>
		void write_floating_point(T const &value) const {
			write(reinterpret_cast<word_t const *>(&value), sizeof(T));
			//todo check correctness
		}

		template<typename T>
		std::vector<T> readArray() const {
			int32_t len = read_integral<int32_t>();
			MY_ASSERT_MSG(len >= 0, "read null array(length = " + std::to_string(len) + ")");
			std::vector<T> result(len);
			read(reinterpret_cast<word_t *>(result.data()), sizeof(T) * len);
			return result;
		}

		template<typename T>
		std::vector<value_or_wrapper<T>> readArray(std::function<value_or_wrapper<T>()> reader) const {
			int32_t len = read_integral<int32_t>();
			std::vector<value_or_wrapper<T>> result(len);
			for (auto &x : result) {
				x = std::move(reader());
			}
			return result;
		}

		template<typename T>
		void writeArray(std::vector<T> const &array) const {
			write_integral<int32_t>(static_cast<int32_t>(array.size()));
			write(reinterpret_cast<word_t const *>(array.data()), sizeof(T) * array.size());
		}

		template<typename T>
		void writeArray(std::vector<T> const &array, std::function<void(T const &)> writer) const {
			write_integral<int32_t>(array.size());
			for (auto const &e : array) {
				writer(e);
			}
		}

		template<typename T>
		void writeArray(std::vector<Wrapper<T>> const &array, std::function<void(T const &)> writer) const {
			write_integral<int32_t>(array.size());
			for (auto const &e : array) {
				writer(*e);
			}
		}

		void readByteArrayRaw(ByteArray &array) const;

		void writeByteArrayRaw(ByteArray const &array) const;

		//    std::string readString() const;

		//    void writeString(std::string const &value) const;

		bool readBool() const;

		void writeBool(bool value) const;

		std::wstring readWString() const;

		void writeWString(std::wstring const &value) const;

		void writeWString(Wrapper<std::wstring> const &value) const;

		template<typename T>
		T readEnum() const {
			int32_t x = read_integral<int32_t>();
			return static_cast<T>(x);
		}

		template<typename T>
		void writeEnum(T const &x) const {
			write_integral<int32_t>(static_cast<int32_t>(x));
		}

		template<typename T, typename F,
				typename = typename std::enable_if_t<util::is_same_v<typename std::result_of_t<F()>, T>>>
		opt_or_wrapper<T> readNullable(F &&reader) const {
			bool nullable = !readBool();
			if (nullable) {
				return {};
			}
			return {reader()};
		}

		template<typename T, typename F,
				typename = typename std::enable_if_t<util::is_same_v<typename std::result_of_t<F()>, Wrapper<T>>>>
		Wrapper<T> readNullable(F &&reader) const {
			bool nullable = !readBool();
			if (nullable) {
				return {};
			}
			return reader();
		}

		template<typename T, typename = typename std::enable_if_t<!std::is_abstract<T>::value>>
		void writeNullable(tl::optional<T> const &value, std::function<void(T const &)> writer) const {
			if (!value) {
				writeBool(false);
			} else {
				writeBool(true);
				writer(*value);
			}
		}

		template<typename T, typename F>
		typename std::enable_if_t<!util::is_invocable_v<F, Wrapper<T>>>
		writeNullable(Wrapper<T> const &value, F &&writer) const {
			if (!value) {
				writeBool(false);
			} else {
				writeBool(true);
				writer(*value);
			}
		}

		template<typename T, typename F>
		typename std::enable_if_t<util::is_invocable_v<F, Wrapper<T>>>
		writeNullable(Wrapper<T> const &value, F &&writer) const {
			if (!value) {
				writeBool(false);
			} else {
				writeBool(true);
				writer(value);
			}
		}

		ByteArray getArray() const &;

		ByteArray getArray() &&;

		ByteArray getRealArray() const &;

		ByteArray getRealArray() &&;

		word_t const *data() const;

		word_t *data();

		size_t size() const;
	};
}


#endif //RD_CPP_UNSAFEBUFFER_H
