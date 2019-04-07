#ifndef RD_CPP_RDPROPERTY_H
#define RD_CPP_RDPROPERTY_H


#include "RdPropertyBase.h"
#include "Polymorphic.h"
#include "ISerializable.h"

#pragma warning( push )
#pragma warning( disable:4250 )

namespace rd {
	template<typename T, typename S = Polymorphic<T>>
	class RdProperty final : public RdPropertyBase<T, S>, public ISerializable {
	public:
		using value_type = T;

		//region ctor/dtor

		RdProperty() = default;

		RdProperty(RdProperty const &) = delete;

		RdProperty &operator=(RdProperty const &) = delete;

		RdProperty(RdProperty &&) = default;

		RdProperty &operator=(RdProperty &&) = default;

		explicit RdProperty(T const &value) : RdPropertyBase<T, S>(value) {}

		explicit RdProperty(T &&value) : RdPropertyBase<T, S>(std::move(value)) {}

		virtual ~RdProperty() = default;
		//endregion

		static RdProperty<T, S> read(SerializationCtx const &ctx, Buffer const &buffer) {
			RdId id = RdId::read(buffer);
			bool not_null = buffer.readBool();//not null/
			(void) not_null;
			auto value = S::read(ctx, buffer);
			RdProperty<T, S> property;
			property.value = std::move(value);
			withId(property, id);
			return property;
		}

		void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
			this->rdid.write(buffer);
			buffer.writeBool(true);
			S::write(ctx, buffer, this->get());
		}

		void advise(Lifetime lifetime, std::function<void(const T &)> handler) const override {
			RdPropertyBase<T, S>::advise(lifetime, std::move(handler));
		}

		RdProperty<T, S> &slave() {
			this->is_master = false;
			return *this;
		}


		void identify(Identities const &identities, RdId const &id) const override {
			RdBindableBase::identify(identities, id);
			if (!this->optimize_nested && this->has_value()) {
				identifyPolymorphic(this->get(), identities, identities.next(id));
			}
		}

		friend bool operator==(const RdProperty &lhs, const RdProperty &rhs) {
			return &lhs == &rhs;
		}

		friend bool operator!=(const RdProperty &lhs, const RdProperty &rhs) {
			return !(rhs == lhs);
		}
	};
}

#pragma warning( pop )

static_assert(std::is_move_constructible<rd::RdProperty<int> >::value, "Is move constructible from RdProperty<int>");

#endif //RD_CPP_RDPROPERTY_H
