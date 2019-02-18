//
// Created by jetbrains on 30.07.2018.
//

#include "Buffer.h"

#include <string>
#include <algorithm>

namespace rd {
	Buffer::Buffer(int32_t initialSize) {
		byteBufferMemoryBase.resize(initialSize);
		size_ = initialSize;
	}

	Buffer::Buffer(const ByteArray &array, int32_t offset) :
			byteBufferMemoryBase(array), offset(offset),
			size_(array.size()) {}

	int32_t Buffer::get_position() const {
		return offset;
	}

	void Buffer::set_position(int32_t value) const {
		offset = value;
	}

	void Buffer::check_available(int32_t moreSize) const {
		if (offset + moreSize > size_) {
			throw std::out_of_range(
					"Expected " + std::to_string(moreSize) + " bytes in buffer, only" + std::to_string(size_ - offset) +
					"available");
		}
	}

	void Buffer::read(word_t *dst, size_t size) const {
		check_available(size);
		//    memcpy(dst, &byteBufferMemoryBase[offset], size);
		std::copy(&byteBufferMemoryBase[offset], &byteBufferMemoryBase[offset] + size, dst);
		offset += size;
	}

	void Buffer::write(const word_t *src, size_t size) const {
		require_available(size);
		//    memcpy(&byteBufferMemoryBase[offset], src, size);
		std::copy(src, src + size, &byteBufferMemoryBase[offset]);
		offset += size;
	}

	void Buffer::require_available(int32_t moreSize) const {
		if (offset + moreSize >= size_) {
			int32_t newSize = (std::max)(size_ * 2, offset + moreSize);
			byteBufferMemoryBase.resize(newSize);
			size_ = newSize;
		}
	}

	void Buffer::rewind() const {
		set_position(0);
	}

	Buffer::ByteArray Buffer::getArray() const &{
		return byteBufferMemoryBase;
	}

	Buffer::ByteArray Buffer::getArray() &&{
		return std::move(byteBufferMemoryBase);
	}

	Buffer::ByteArray Buffer::getRealArray() const &{
		auto res = getArray();
		res.resize(offset);
		return res;
	}

	Buffer::ByteArray Buffer::getRealArray() &&{
		auto res = getArray();
		res.resize(offset);
		return res;
	}

	const Buffer::word_t *Buffer::data() const {
		return byteBufferMemoryBase.data();
	}

	Buffer::word_t *Buffer::data() {
		return byteBufferMemoryBase.data();
	}

	size_t Buffer::size() const {
		return size_;
	}

	/*std::string Buffer::readString() const {
    auto v = readArray<uint8_t>();
    return std::string(v.begin(), v.end());
}

void Buffer::writeString(std::string const &value) const {
    std::vector<uint8_t> v(value.begin(), value.end());
    writeArray<uint8_t>(v);
}*/

	std::wstring Buffer::readWString() const {
		auto v = readArray<uint16_t>();
		return std::wstring(v.begin(), v.end());
	}

	void Buffer::writeWString(std::wstring const &value) const {
		std::vector<uint16_t> v(value.begin(), value.end());
		writeArray<uint16_t>(v);
	}

	bool Buffer::readBool() const {
		auto res = read_integral<uint8_t>();
		MY_ASSERT_MSG(res == 0 || res == 1, "get byte:" + std::to_string(res) + " instead of 0 or 1");
		return res == 1;
	}

	void Buffer::writeBool(bool value) const {
		write_integral<word_t>(value ? 1 : 0);
	}

	void Buffer::readByteArrayRaw(ByteArray &array) const {
		read(array.data(), array.size());
	}

	void Buffer::writeByteArrayRaw(const ByteArray &array) const {
		write(array.data(), array.size());
	}

	/*tl::optional<std::wstring> Buffer::readNullableWString() const {
    int32_t len = read_integral<int32_t>();
    if (len < 0) {
        return tl::nullopt;
    }
    std::wstring result;
    result.resize(len);
    read(reinterpret_cast<word_t *>(&result[0]), sizeof(wchar_t) * len);
    return result;
}

void Buffer::writeNullableWString(tl::optional<std::wstring> const &value) const {
    if (!value.has_value()) {
        write_integral<int32_t>(-1);
        return;
    }
    writeWString(value.value());
}*/


}
