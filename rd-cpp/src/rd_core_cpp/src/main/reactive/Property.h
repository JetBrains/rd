#ifndef RD_CPP_CORE_PROPERTY_H
#define RD_CPP_CORE_PROPERTY_H

#include "IProperty.h"
#include "SignalX.h"
#include "core_util.h"

namespace rd {

	/**
	 * \brief complete class which has \a Property<T> 's properties.
	 * \tparam T type of stored value (may be abstract)
	 */
	template<typename T>
	class Property : public IProperty<T> {
	public:
		//region ctor/dtor

		Property() = default;

		Property(Property &&other) = default;

		Property &operator=(Property &&other) = default;

		virtual ~Property() = default;

		template <typename F>
		explicit Property(F &&value) : IProperty<T>(std::forward<F>(value)) {}
		//endregion


		T const &get() const override {
			RD_ASSERT_THROW_MSG(this->has_value(), "get of uninitialized value from property");
			return *(this->value);
		}

		void set(value_or_wrapper<T> new_value) const override {
			if (!this->has_value() || (this->get() != wrapper::get<T>(new_value))) {
				if (this->has_value()) {
					this->before_change.fire(*(this->value));
				}
				this->value = std::move(new_value);
				this->change.fire(*(this->value));
			}
		}

		friend bool operator==(const Property &lhs, const Property &rhs) {
			return &lhs == &rhs;
		}

		friend bool operator!=(const Property &lhs, const Property &rhs) {
			return !(rhs == lhs);
		}
	};
}

static_assert(std::is_move_constructible<rd::Property<int>>::value, "Is not move constructible from Property<int>");

#endif //RD_CPP_CORE_PROPERTY_H
