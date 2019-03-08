#ifndef RD_CPP_INTERNROOT_H
#define RD_CPP_INTERNROOT_H

#include "RdReactiveBase.h"
#include "InternScheduler.h"
#include "ISerializable.h"
#include "Lifetime.h"
#include "wrapper.h"

#include "tsl/ordered_map.h"

#include <vector>
#include <string>
#include <mutex>

namespace rd {
	//region predeclared

	class Identities;

	//endregion

	class rInternRoot final : public RdReactiveBase {
	private:
		template<typename T>
		static std::vector<value_or_wrapper<T>> myItemsList;

		template<typename T>
		static tsl::ordered_map<int32_t, value_or_wrapper<T>> otherItemsList;
		template<typename T>
		static tsl::ordered_map<value_or_wrapper<T>, int32_t> inverseMap;

		mutable InternScheduler intern_scheduler;

		mutable std::mutex lock;

		template<typename T>
		void set_interned_correspondence(int32_t id, T &&value) const {
			MY_ASSERT_MSG(!is_index_owned(id), "Setting interned correspondence for object that we should have written, bug?")

			otherItemsList<T>[id / 2] = std::forward<T>(value);
			inverseMap<T>[value] = id;
		}

		static constexpr bool is_index_owned(int32_t id) {
			return static_cast<bool>(id & 1);
		}

	public:
		//region ctor/dtor

		InternRoot();
		//endregion

		template<typename T>
		int32_t intern_value(T const &value) const;

		template<typename T>
		value_or_wrapper<T> un_intern_value(int32_t id) const;

		IScheduler * get_wire_scheduler() const override;

		void bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const override;

		void identify(const Identities &identities, RdId const &id) const override;

		void on_wire_received(Buffer buffer) const override;
	};

	template<typename T>
	std::vector<value_or_wrapper<T>> InternRoot::myItemsList = {};

	template<typename T>
	tsl::ordered_map<int32_t, value_or_wrapper<T>> InternRoot::otherItemsList = {};

	template<typename T>
	tsl::ordered_map<value_or_wrapper<T>, int32_t> InternRoot::inverseMap = {};

	template<typename T>
	value_or_wrapper<T> InternRoot::un_intern_value(int32_t id) const {
		return {};
//		return is_index_owned(id) ? myItemsList<T>[id / 2] : otherItemsList<T>[id / 2];
	}

	template<typename T>
	int32_t InternRoot::intern_value(const T &value) const {
		auto it = inverseMap<T>.find(value);
		int32_t index = 0;
		if (it == inverseMap<T>.end()) {
			get_protocol()->get_wire()->send(this->rdid, [this, &index, &value](Buffer const &buffer) {

//				Polymorphic<T>::write(get_serialization_context(), buffer, value);
				{
					std::lock_guard<decltype(lock)> guard(lock);
					index = static_cast<int32_t>(myItemsList<T>.size()); //todo change to global counter
//					myItemsList<T>.emplace_back(value);
				}
				buffer.write_integral<int32_t>(index);
			});
		}
		/*if (inverseMap<T>.count(value) == 0) {
			inverseMap<T>[value] = index;
		}*/
		return index;
	}
}


#endif //RD_CPP_INTERNROOT_H
