//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_UNSAFEBUFFER_H
#define RD_CPP_UNSAFEBUFFER_H

#include "core_util.h"
#include "optional.hpp"

#include <utility>
#include <vector>
#include <type_traits>
#include <functional>

struct is_bool {
};
struct not_bool {
};

template<typename T>
struct bool_tag {
    using type = not_bool;
};

template<>
struct bool_tag<bool> {
    using type = is_bool;
};

class Buffer {
public:
    using word_t = uint8_t;

    using ByteArray = std::vector<word_t>;
protected:

    mutable ByteArray byteBufferMemoryBase;
    mutable int32_t offset = 0;
    mutable int32_t size_ = 0;

    void require_available(int32_t size) const;

    //read
    void read(word_t *dst, size_t size) const;

    //write
    void write(const word_t *src, size_t size) const;

    template<typename T>
    T read_pod_tag(is_bool) const {
        auto res = read_pod<word_t>();
        MY_ASSERT_MSG(res == 0 || res == 1, "get byte:" + to_string(res) + " instead of 0 or 1");
        return res == 1;
    }

    template<typename T>
    T read_pod_tag(not_bool) const {
        T result;
        read(reinterpret_cast<word_t *>(&result), sizeof(T));
        return result;
    }

    template<typename T>
    void write_pod_tag(T const &value, is_bool) const {
        write_pod<word_t>(value ? 1 : 0);
    }

    template<typename T>
    void write_pod_tag(T const &value, not_bool) const {
        write(reinterpret_cast<word_t const *>(&value), sizeof(T));
    }

public:
    //region ctor/dtor

    explicit Buffer(int32_t initialSize = 10); //todo

    explicit Buffer(const ByteArray &array, int32_t offset = 0)
            : byteBufferMemoryBase(array), offset(offset), size_(array.size()) {}

    Buffer(Buffer const &) = delete;

    Buffer(Buffer &&) = default;
    //endregion

    int32_t get_position() const;

    void set_position(int32_t value) const;

    void check_available(int32_t moreSize) const;

    void rewind() const;

    template<typename T, typename = typename std::enable_if<std::is_integral<T>::value, T>::type>
    T read_pod() const {
        return this->read_pod_tag<T>(typename bool_tag<T>::type());
    }

    template<typename T, typename = typename std::enable_if<std::is_integral<T>::value>::type>
    void write_pod(T const &value) const {
        this->write_pod_tag<T>(value, typename bool_tag<T>::type());
    }

    template<typename T>
    std::vector<T> readArray() const {
        int32_t len = read_pod<int32_t>();
        std::vector<T> result(len);
        read(reinterpret_cast<word_t *>(result.data()), sizeof(T) * len);
        return result;
    }

    template<typename T>
    std::vector<T> readArray(std::function<T()> reader) const {
        int32_t len = read_pod<int32_t>();
        std::vector<T> result(len);
        for (auto &x : result) {
            x = reader();
        }
        return result;
    }

    template<typename T>
    void writeArray(std::vector<T> const &array) const {
        write_pod<int32_t>(array.size());
        write(reinterpret_cast<word_t const *>(array.data()), sizeof(T) * array.size());
    }

    template<typename T>
    void writeArray(std::vector<T> const &array, std::function<void(T const &)> writer) const {
        write_pod<int32_t>(array.size());
        for (auto const &e : array) {
            writer(e);
        }
    }

    template<typename T>
    void writeByteArrayRaw(std::vector<T> const &array) const {
        write(array.data(), sizeof(T) * array.size());
    }

    std::string readString() const;

    void writeString(std::string const &value) const;

    template<typename T>
    T readEnum() const {
        int32_t x = read_pod<int32_t>();
        return static_cast<T>(x);
    }

    template<typename T>
    void writeEnum(T const &x) const {
        write_pod<int32_t>(static_cast<int32_t>(x));
    }

    template<typename T>
    tl::optional<T> readNullable(std::function<T()> reader) const {
        bool nullable = !read_pod<bool>();
        if (nullable) {
            return tl::nullopt;
        }
        return reader();
    }

    template<typename T>
    void writeNullable(tl::optional<T> const &value, std::function<void(T const &)> writer) const {
        if (!value.has_value()) {
            write_pod<bool>(false);
        } else {
            write_pod<bool>(true);
            writer(*value);
        }
    }

    ByteArray getArray();

    ByteArray getRealArray();

    word_t const *data() const;

    word_t *data();

    size_t size() const;

};


#endif //RD_CPP_UNSAFEBUFFER_H
