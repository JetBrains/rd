#include "Serializers.h"

#include "AbstractPolymorphic.h"

namespace rd {
	RdId Serializers::real_rd_id(const IUnknownInstance &value) {
		return value.unknownId;
	}

	RdId Serializers::real_rd_id(const IPolymorphicSerializable &value) {
		return RdId(util::getPlatformIndependentHash(value.type_name()));
	}

	RdId Serializers::real_rd_id(const std::wstring &value) {
		return RdId(10);
		//todo constant
	}

	void Serializers::real_write(SerializationCtx const &ctx, Buffer const &buffer, IUnknownInstance const &value) {
		value.unknownId.write(buffer);
	}

	void Serializers::real_write(SerializationCtx const &ctx, Buffer const &buffer, IPolymorphicSerializable const &value) {
		value.write(ctx, buffer);
	}

	void Serializers::real_write(SerializationCtx const &ctx, Buffer const &buffer, std::wstring const &value) {
		Polymorphic<std::wstring>::write(ctx, buffer, value);
	}

	Serializers::Serializers() {
		readers[RdId(10)] = [](SerializationCtx const &ctx, Buffer const &buffer) -> InternedAny {
			return wrapper::make_wrapper<std::wstring>(Polymorphic<std::wstring>::read(ctx, buffer));
		};
	}

	tl::optional<InternedAny> Serializers::readAny(SerializationCtx const &ctx, Buffer const &buffer) const {
		RdId id = RdId::read(buffer);
		if (id.isNull()) {
			return tl::nullopt;
		}
		int32_t size = buffer.read_integral<int32_t>();
		buffer.check_available(static_cast<size_t>(size));

		if (readers.count(id) == 0) {
			throw std::invalid_argument("no reader");
		}
		auto const &reader = readers.at(id);
		return reader(ctx, buffer);
	}
}
