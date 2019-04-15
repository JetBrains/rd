#ifndef RD_CPP_IPROPERTY_H
#define RD_CPP_IPROPERTY_H

#include "SignalX.h"
#include "Lifetime.h"
#include "IPropertyBase.h"
#include "wrapper.h"

#include <functional>

namespace rd {
	template<typename T>
	class IProperty : public IPropertyBase<T> {
	protected:
		using WT = value_or_wrapper<T>;
	public:

		//region ctor/dtor

		IProperty() = default;

		IProperty(IProperty &&other) = default;

		IProperty &operator=(IProperty &&other) = default;

		explicit IProperty(T const &value) : IPropertyBase<T>(value) {}

		explicit IProperty(T &&value) : IPropertyBase<T>(std::move(value)) {}

		virtual ~IProperty() = default;
		//endregion

		virtual T const &get() const = 0;

		void advise0(Lifetime lifetime, std::function<void(T const &)> handler, Signal<T> const &signal) const {
			if (lifetime.is_terminated()) {
				return;
			}
			signal.advise(std::move(lifetime), handler);
			if (this->has_value()) {
				handler(this->get());
			}
		}

		void advise_before(Lifetime lifetime, std::function<void(T const &)> handler) const override {
			advise0(lifetime, handler, this->before_change);
		}

		void advise(Lifetime lifetime, std::function<void(T const &)> handler) const override {
			advise0(std::move(lifetime), std::move(handler), this->change);
		}

		virtual void set(value_or_wrapper<T>) const = 0;
	};
}


#endif //RD_CPP_IPROPERTY_H
