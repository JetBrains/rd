#ifndef RD_CPP_INTERNROOT_H
#define RD_CPP_INTERNROOT_H


#include "RdReactiveBase.h"
#include "InternScheduler.h"
#include "Lifetime.h"
#include "wrapper.h"
#include "RdAny.h"

#include "tsl/ordered_map.h"

#include <vector>
#include <string>
#include <mutex>


#pragma warning( push )
#pragma warning( disable:4250 )

namespace rd {
	//region predeclared

	class Identities;

	//endregion

	class InternRoot final : public RdReactiveBase {
	private:
		// template<typename T>
		mutable std::vector<RdAny> myItemsList;

		// template<typename T>
		mutable tsl::ordered_map<int32_t, RdAny> otherItemsList;
		// template<typename T>
		mutable tsl::ordered_map<RdAny, int32_t, any::TransparentHash, any::TransparentKeyEqual> inverseMap;

		mutable InternScheduler intern_scheduler;

		mutable std::mutex lock;

		template<typename T>
		void set_interned_correspondence(int32_t id, T &&value) const {
			MY_ASSERT_MSG(!is_index_owned(id), "Setting interned correspondence for object that we should have written, bug?")

			otherItemsList[id / 2] = value;
			inverseMap[std::forward<T>(value)] = id;
		}

		static constexpr bool is_index_owned(int32_t id) {
			return !static_cast<bool>(id & 1);
		}

	public:
		//region ctor/dtor

		InternRoot();
		//endregion

		template<typename T>
		int32_t intern_value(const value_or_wrapper<T> &value) const;

		template<typename T>
		value_or_wrapper<T> un_intern_value(int32_t id) const;

		IScheduler *get_wire_scheduler() const override;

		void bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const override;

		void identify(const Identities &identities, RdId const &id) const override;

		void on_wire_received(Buffer buffer) const override;
	};
}

#pragma warning( pop )

#include "AnySerializer.h"

namespace rd {
	/*template<typename T>
	std::vector<value_or_wrapper<T>> InternRoot::myItemsList = {};

	template<typename T>
	tsl::ordered_map<int32_t, value_or_wrapper<T>> InternRoot::otherItemsList = {};

	template<typename T>
	tsl::ordered_map<value_or_wrapper<T>, int32_t> InternRoot::inverseMap = {};*/

	template<typename T>
	value_or_wrapper<T> InternRoot::un_intern_value(int32_t id) const {
		auto const& v = (any::get<T>(is_index_owned(id) ? myItemsList[id / 2] : otherItemsList[id / 2]));
		return wrapper::get(v);
	}

	template<typename T>
	int32_t InternRoot::intern_value(const value_or_wrapper<T> &value) const {
		auto it = inverseMap.find(value);
		int32_t index = 0;
		if (it == inverseMap.end()) {
			get_protocol()->get_wire()->send(this->rdid, [this, &index, &value](Buffer const &buffer) {
				AnySerializer::write<T>(get_serialization_context(), buffer, wrapper::get<T>(value));
				{
					std::lock_guard<decltype(lock)> guard(lock);
					index = static_cast<int32_t>(myItemsList.size()) * 2; //todo change to global counter
					myItemsList.emplace_back(value);
				}
				buffer.write_integral<int32_t>(index);
			});
		} else {
			index = it->second;
		}
		if (inverseMap.count(value) == 0) {
			inverseMap[value] = index;
		}
		return index;
	}
}


#endif //RD_CPP_INTERNROOT_H
