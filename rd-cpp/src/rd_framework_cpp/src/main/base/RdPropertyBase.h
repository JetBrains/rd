#ifndef RD_CPP_RDPROPERTYBASE_H
#define RD_CPP_RDPROPERTYBASE_H


#include "base/RdReactiveBase.h"
#include "serialization/Polymorphic.h"
#include "reactive/Property.h"

#pragma warning( push )
#pragma warning( disable:4250 )

namespace rd {
	template<typename T, typename S = Polymorphic <T>>
	class RdPropertyBase : public RdReactiveBase, public Property<T> {
	protected:
		using WT = typename IProperty<T>::WT;
		//mastering
		mutable int32_t master_version = 0;
		mutable bool default_value_changed = false;

		//init
	public:
		mutable bool optimize_nested = false;

		bool is_master = false;

		//region ctor/dtor

		RdPropertyBase() = default;

		RdPropertyBase(RdPropertyBase const &) = delete;

		RdPropertyBase(RdPropertyBase &&other) = default;

		RdPropertyBase &operator=(RdPropertyBase &&other) = default;

		template <typename F>
		explicit RdPropertyBase(F &&value) : Property<T>(std::forward<F>(value)) {}

		virtual ~RdPropertyBase() = default;
		//endregion

		bool is_default_value_changed() const {
			return default_value_changed;
		}
		
		void init(Lifetime lifetime) const override {
			RdReactiveBase::init(lifetime);


			if (!optimize_nested) {
				this->change.advise(lifetime, [this](T const &v) {
					if (is_local_change) {
						if (this->has_value()) {
							const IProtocol *iProtocol = get_protocol();
							const Identities *identity = iProtocol->get_identity();
							identifyPolymorphic(v, *identity, identity->next(rdid));
						}
					}
				});
			}

			advise(lifetime, [this](T const &v) {
				if (!is_local_change) {
					return;
				}
				if (is_master) {
					master_version++;
				}
				get_wire()->send(rdid, [this, &v](Buffer &buffer) {
					buffer.write_integral<int32_t>(master_version);
					S::write(this->get_serialization_context(), buffer, v);
					logSend.trace("SEND property " + to_string(location) + " + " + to_string(rdid) +
								  ":: ver = " + std::to_string(master_version) +
								  ", value = " + to_string(v));
				});
			});

			get_wire()->advise(lifetime, this);

			if (!optimize_nested) {
				this->view(lifetime, [this](Lifetime lf, T const &v) {
					if (this->has_value()) {
						bindPolymorphic(v, lf, this, "$");
					}
				});
			}
		}

		void on_wire_received(Buffer buffer) const override {
			int32_t version = buffer.read_integral<int32_t>();
			WT v = S::read(this->get_serialization_context(), buffer);

			bool rejected = is_master && version < master_version;
			logSend.trace("RECV property " + to_string(location) + " " + to_string(rdid) +
						  ":: oldver=%d, ver=%d, value = " + to_string(v) + (rejected ? ">> REJECTED" : ""),
						  master_version, version);
			if (rejected) {
				return;
			}
			master_version = version;

			Property<T>::set(std::move(v));
		}

		void advise(Lifetime lifetime, std::function<void(T const &)> handler) const override {
			if (is_bound()) {
				assert_threading();
			}
			Property<T>::advise(lifetime, handler);
		}

		void set(value_or_wrapper <T> new_value) const override {
			this->local_change([this, new_value = std::move(new_value)]() mutable {
				this->default_value_changed = true;
				Property<T>::set(std::move(new_value));
			});
		}

		friend bool operator==(const RdPropertyBase &lhs, const RdPropertyBase &rhs) {
			return &lhs == &rhs;
		}

		friend bool operator!=(const RdPropertyBase &lhs, const RdPropertyBase &rhs) {
			return !(rhs == lhs);
		}
	};
}

#pragma warning( pop )

static_assert(std::is_move_constructible<rd::RdPropertyBase<int> >::value,
			  "Is move constructible from RdPropertyBase<int>");

#endif //RD_CPP_RDPROPERTYBASE_H
