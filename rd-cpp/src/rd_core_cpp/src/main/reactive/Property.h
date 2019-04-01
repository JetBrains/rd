//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_PROPERTY_H
#define RD_CPP_CORE_PROPERTY_H

#include "IProperty.h"
#include "SignalX.h"
#include "core_util.h"

namespace rd {
	template<typename T>
	class Property : public IProperty<T> {
	public:
		//region ctor/dtor

		Property() = default;

		Property(Property &&other) = default;

		Property &operator=(Property &&other) = default;

		virtual ~Property() = default;

		explicit Property(T const &value) : IProperty<T>(value) {}

		explicit Property(T &&value) : IProperty<T>(std::move(value)) {}
		//endregion


		T const &get() const override {
			MY_ASSERT_THROW_MSG(this->has_value(), "get of uninitialized value from property");
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
