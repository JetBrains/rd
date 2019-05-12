#ifndef RD_CPP_IVIEWABLESET_H
#define RD_CPP_IVIEWABLESET_H

#include "LifetimeDefinition.h"
#include "interfaces.h"
#include "core_util.h"
#include "viewable_collections.h"

#include "tsl/ordered_map.h"

#include <unordered_map>

namespace rd {
	template<typename T>
	class IViewableSet : public IViewable<T> {
	protected:
		using WT = value_or_wrapper<T>;
		mutable std::unordered_map<
				Lifetime,
				ordered_map<T const *, LifetimeDefinition, wrapper::TransparentHash<T>, wrapper::TransparentKeyEqual<T>>
		> lifetimes;
	public:

		//region ctor/dtor

		IViewableSet() = default;

		IViewableSet(IViewableSet &&) = default;

		IViewableSet &operator=(IViewableSet &&) = default;

		virtual ~IViewableSet() = default;
		//endregion

		class Event {
		public:
			Event(AddRemove kind, T const *value) : kind(kind), value(value) {}

			AddRemove kind;
			T const *value;
		};

		void advise(Lifetime lifetime, std::function<void(AddRemove, T const &)> handler) const {
			this->advise(lifetime, [handler](Event e) {
				handler(e.kind, *e.value);
			});
		}


		void view(Lifetime lifetime, std::function<void(Lifetime, T const &)> handler) const override {
			advise(lifetime, [this, lifetime, handler](AddRemove kind, T const &key) {
				switch (kind) {
					case AddRemove::ADD: {
						/*auto const &[it, inserted] = lifetimes[lifetime].emplace(key, LifetimeDefinition(lifetime));*/
						auto const &it = lifetimes[lifetime].emplace(&key, LifetimeDefinition(lifetime));
						RD_ASSERT_MSG(it.second,
									  "lifetime definition already exists in viewable set by key:" + to_string(key));
						handler(it.first->second.lifetime, key);
						break;
					}
					case AddRemove::REMOVE: {
						RD_ASSERT_MSG(lifetimes.at(lifetime).count(key) > 0,
									  "attempting to remove non-existing lifetime in viewable set by key:" +
									  to_string(key));
						LifetimeDefinition def = std::move(lifetimes.at(lifetime).at(key));
						lifetimes.at(lifetime).erase(key);
						def.terminate();
						break;
					}
				}
			});
		}

		virtual void advise(Lifetime lifetime, std::function<void(Event)> handler) const = 0;

		virtual bool add(WT) const = 0;

		virtual bool addAll(std::vector<WT> elements) const = 0;

		virtual void clear() const = 0;

		virtual bool remove(T const &) const = 0;

		virtual size_t size() const = 0;

		virtual bool contains(T const &) const = 0;

		virtual bool empty() const = 0;
	};
}

static_assert(std::is_move_constructible<rd::IViewableSet<int>::Event>::value,
			  "Is move constructible from IViewableSet<int>::Event");

#endif //RD_CPP_IVIEWABLESET_H
