#ifndef RD_CPP_UNSAFEBUFFER_H
#define RD_CPP_UNSAFEBUFFER_H

#include "core_util.h"
#include "wrapper.h"

#include <vector>
#include <type_traits>
#include <functional>
#include <memory>

namespace rd {
	class Buffer final {
	public:
		using word_t = uint8_t;

		using Allocator = std::allocator<word_t>;

		using ByteArray = std::vector<word_t, Allocator>;
	private:
		template<int>
		friend std::wstring read_wstring_spec(Buffer const&);

		template<int>
		friend void write_wstring_spec(Buffer const&, std::wstring const&);

		mutable ByteArray data_;

		mutable size_t offset = 0;

		void require_available(size_t size) const;

		//read
		void read(word_t *dst, size_t size) const;

		//write
		void write(const word_t *src, size_t size) const;


	public:

		//region ctor/dtor

		Buffer();

		explicit Buffer(size_t initial_size);

		explicit Buffer(const ByteArray &array, size_t offset = 0);

		explicit Buffer(ByteArray &&array, size_t offset = 0);

		Buffer(Buffer const &) = delete;

		Buffer& operator=(Buffer const &) = delete;

		Buffer(Buffer &&) = default;
		Buffer& operator=(Buffer &&) = default;

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
		}

		template<typename T, typename = typename std::enable_if_t<std::is_floating_point<T>::value>>
		void write_floating_point(T const &value) const {
			write(reinterpret_cast<word_t const *>(&value), sizeof(T));
		}

		template<typename T>
		std::vector<T> read_array() const {
			int32_t len = read_integral<int32_t>();
			RD_ASSERT_MSG(len >= 0, "read null array(length = " + std::to_string(len) + ")");
			std::vector<T> result(len);
			read(reinterpret_cast<word_t *>(result.data()), sizeof(T) * len);
			return result;
		}

		template<typename T>
		std::vector<value_or_wrapper<T>> read_array(std::function<value_or_wrapper<T>()> reader) const {
			int32_t len = read_integral<int32_t>();
			std::vector<value_or_wrapper<T>> result(len);
			for (auto &x : result) {
				x = std::move(reader());
			}
			return result;
		}

		template<typename T>
		void write_array(std::vector<T> const &array) const {
			write_integral<int32_t>(static_cast<int32_t>(array.size()));
			write(reinterpret_cast<word_t const *>(array.data()), sizeof(T) * array.size());
		}

		template<typename T>
		void write_array(std::vector<T> const &array, std::function<void(T const &)> writer) const {
			write_integral<int32_t>(array.size());
			for (auto const &e : array) {
				writer(e);
			}
		}

		template<typename T>
		void write_array(std::vector<Wrapper<T>> const &array, std::function<void(T const &)> writer) const {
			write_integral<int32_t>(array.size());
			for (auto const &e : array) {
				writer(*e);
			}
		}

		void read_byte_array(ByteArray &array) const;
		
		void read_byte_array_raw(ByteArray &array) const;

		void write_byte_array_raw(ByteArray const &array) const;

		//    std::string readString() const;

		//    void writeString(std::string const &value) const;

		bool read_bool() const;

		void write_bool(bool value) const;

		std::wstring read_wstring() const;

		void write_wstring(std::wstring const &value) const;

		void write_wstring(Wrapper<std::wstring> const &value) const;

		template<typename T>
		T read_enum() const {
			int32_t x = read_integral<int32_t>();
			return static_cast<T>(x);
		}

		template<typename T>
		void write_enum(T const &x) const {
			write_integral<int32_t>(static_cast<int32_t>(x));
		}

		template<typename T, typename F,
				typename = typename std::enable_if_t<util::is_same_v<typename std::result_of_t<F()>, T>>>
		opt_or_wrapper<T> read_nullable(F &&reader) const {
			bool nullable = !read_bool();
			if (nullable) {
				return {};
			}
			return {reader()};
		}

		template<typename T, typename F,
				typename = typename std::enable_if_t<util::is_same_v<typename std::result_of_t<F()>, Wrapper<T>>>>
		Wrapper<T> read_nullable(F &&reader) const {
			bool nullable = !read_bool();
			if (nullable) {
				return {};
			}
			return reader();
		}

		template<typename T>
		typename std::enable_if_t<!std::is_abstract<T>::value>
		write_nullable(optional<T> const &value, std::function<void(T const &)> writer) const {
			if (!value) {
				write_bool(false);
			} else {
				write_bool(true);
				writer(*value);
			}
		}

		template<typename T, typename F>
		typename std::enable_if_t<!util::is_invocable_v<F, Wrapper<T>>>
		write_nullable(Wrapper<T> const &value, F &&writer) const {
			if (!value) {
				write_bool(false);
			} else {
				write_bool(true);
				writer(*value);
			}
		}

		template<typename T, typename F>
		typename std::enable_if_t<util::is_invocable_v<F, Wrapper<T>>>
		write_nullable(Wrapper<T> const &value, F &&writer) const {
			if (!value) {
				write_bool(false);
			} else {
				write_bool(true);
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

		ByteArray &get_data();
	};
}


#endif //RD_CPP_UNSAFEBUFFER_H
