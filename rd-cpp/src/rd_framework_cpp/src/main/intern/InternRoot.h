#ifndef RD_CPP_INTERNROOT_H
#define RD_CPP_INTERNROOT_H

#include "RdReactiveBase.h"
#include "InternScheduler.h"
#include "ISerializable.h"
#include "Lifetime.h"
#include "wrapper.h"

#include <vector>
#include <string>
#include <any>



namespace rd {
	//region predeclared

	class Identities;

	//endregion

	class InternRoot final : public RdReactiveBase {
	private:
		// template<
		/*mutable std::vector<std::any> myItemsList;
		mutable std::unordered_map<int32_t, std::any> otherItemsList;
		mutable std::unordered_map<std::any, int32_t> inverseMap;*/

		mutable InternScheduler intern_scheduler;

		void set_interned_correspondence(int32_t id, Wrapper<IPolymorphicSerializable> wrapper) const;

		static constexpr bool is_index_owned(int32_t id) {
			return static_cast<bool>(id & 1);
		}

	public:
		//region ctor/dtor

		InternRoot();
		//endregion

		template<typename T>
		int32_t intern_value(T const &value) const {
//			auto it = inverseMap.find(value);
			int32_t index = 0;
//			if (it == inverseMap.end()) {
				get_protocol()->wire->send(this->rdid, [this, &index, &value](Buffer const &buffer) {
//					NullableSerializer<Polymorphic<T>>::write(get_serialization_context(), buffer, value);
					{
//						index = myItemsList.size();
//						myItemsList.emplace_back(value);
					}
					buffer.write_integral<int32_t>(index);
				});
//			}
//			if (inverseMap.count(value) == 0) {
//				inverseMap[value] = index;
//			}
			return index;
		}


		template<typename T>
		T un_intern_value(int32_t id) const {
//			return std::any_cast<T>(is_index_owned(id) ? myItemsList[id / 2] : otherItemsList[id / 2]);
		}

		IScheduler *get_wire_scheduler() const override;

		void bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const override;

		void identify(const Identities &identities, RdId const &id) const override;

		void on_wire_received(Buffer buffer) const override;
	};
}


#endif //RD_CPP_INTERNROOT_H
