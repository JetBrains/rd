//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLE_MAP_H
#define RD_CPP_CORE_VIEWABLE_MAP_H


#include "Logger.h"
#include "IViewableMap.h"
#include "SignalX.h"
#include "util/core_util.h"

#include "tsl/ordered_map.h"

namespace rd {
	template<typename K, typename V>
	class ViewableMap : public IViewableMap<K, V> {
	public:
		using Event = typename IViewableMap<K, V>::Event;
	private:
		using WK = typename IViewableMap<K, V>::WK;
		using WV = typename IViewableMap<K, V>::WV;

		Signal<Event> change;

		using data_t = tsl::ordered_map<Wrapper<K>, Wrapper<V>, wrapper::TransparentHash<K>, wrapper::TransparentKeyEqual<K>>;
		mutable data_t map;
	public:
		//region ctor/dtor

		ViewableMap() = default;

		ViewableMap(ViewableMap &&) = default;

		ViewableMap &operator=(ViewableMap &&) = default;

		virtual ~ViewableMap() = default;
		//endregion

		//region iterators

	public:
		class iterator {
			friend class ViewableMap<K, V>;

			mutable typename data_t::iterator it_;

			explicit iterator(const typename data_t::iterator &it) : it_(it) {}

		public:
			using iterator_category = std::random_access_iterator_tag;
			using key_type = K;
			using mapped_type = V;
			using value_type = V;
			using size_type = size_t;
			using difference_type = std::ptrdiff_t;
			using reference = V &;
			using const_reference = const V &;
			using pointer = const K *;
			using const_pointer = const K *;

			iterator(const iterator &other) = default;

			iterator(iterator &&other) noexcept = default;

			iterator &operator=(const iterator &other) = default;

			iterator &operator=(iterator &&other) noexcept = default;

			iterator &operator++() {
				++it_;
				return *this;
			}

			iterator operator++(int) {
				auto it = *this;
				++*this;
				return it;
			}

			iterator &operator--() {
				--it_;
				return *this;
			}

			iterator operator--(int) {
				auto it = *this;
				--*this;
				return it;
			}

			iterator &operator+=(difference_type delta) {
				it_ += delta;
				return *this;
			}

			iterator &operator-=(difference_type delta) {
				it_ -= delta;
				return *this;
			}

			iterator operator+(difference_type delta) const {
				auto it = *this;
				return it += delta;
			}

			iterator operator-(difference_type delta) const {
				auto it = *this;
				return it -= delta;
			}

			difference_type operator-(iterator const &other) const {
				return it_ - other.it_;
			}

			bool operator<(iterator const &other) const noexcept {
				return this->it_ < other.it_;
			}


			bool operator>(iterator const &other) const noexcept {
				return this->it_ > other.it_;
			}


			bool operator==(iterator const &other) const noexcept {
				return this->it_ == other.it_;
			}


			bool operator!=(iterator const &other) const noexcept {
				return !(*this == other);
			}


			bool operator<=(iterator const &other) const noexcept {
				return (this->it_ < other.it_) || (*this == other);
			}


			bool operator>=(iterator const &other) const noexcept {
				return (this->it_ > other.it_) || (*this == other);
			}

			reference operator*() noexcept {
				return *(it_.value());
			}

			const_reference operator*() const noexcept {
				return *(it_.value());
			}

			pointer operator->() const noexcept {
				return it_.value().get();
			}
		};

		using reverse_iterator = std::reverse_iterator<iterator>;

		using const_iterator = iterator;

		using const_reverse_iterator = reverse_iterator;

		iterator begin() { return iterator(map.begin()); }

		const_iterator begin() const { return const_iterator(map.begin()); }

		iterator end() { return iterator(map.end()); }

		const_iterator end() const { return const_iterator(map.end()); }

		reverse_iterator rbegin() { return reverse_iterator(end()); }

		const_reverse_iterator rbegin() const { return const_reverse_iterator(end()); }

		reverse_iterator rend() { return reverse_iterator(begin()); }

		const_reverse_iterator rend() const { return const_reverse_iterator(begin()); }
		//endregion

		void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
			change.advise(lifetime, handler);
			/*for (auto const &[key, value] : map) {*/
			for (auto const &it : map) {
				auto &key = it.first;
				auto &value = it.second;
				handler(Event(typename Event::Add(&(*key), &(*value))));;
			}
		}

		const V *get(K const &key) const override {
			auto it = map.find(key);
			if (it == map.end()) {
				return nullptr;
			}
			return &(*it->second);
		}

		const V *set(WK key, WV value) const override {
			if (map.count(key) == 0) {
				/*auto[it, success] = map.emplace(std::make_unique<K>(std::move(key)), std::make_unique<V>(std::move(value)));*/
				auto node = map.emplace(std::move(key), std::move(value));
				auto &it = node.first;
				auto const &key_ptr = it->first;
				auto const &value_ptr = it->second;
				change.fire(typename Event::Add(&(*key_ptr), &(*value_ptr)));
				return nullptr;
			} else {
				auto it = map.find(key);
				auto const &key_ptr = it->first;
				auto const &value_ptr = it->second;

				if (*value_ptr != wrapper::get<V>(value)) {//todo more effective
					Wrapper<V> old_value = std::move(map.at(key));

					map.at(key_ptr) = Wrapper<V>(std::move(value));
					change.fire(typename Event::Update(&(*key_ptr), &(*old_value), &(*value_ptr)));
				}
				return &*(value_ptr);
			}
		}

		tl::optional<WV> remove(K const &key) const override {
			if (map.count(key) > 0) {
				Wrapper<V> old_value = std::move(map.at(key));
				change.fire(typename Event::Remove(&key, &(*old_value)));
				map.erase(key);
				return wrapper::unwrap<V>(std::move(old_value));
			}
			return tl::nullopt;
		}

		void clear() const override {
			std::vector<Event> changes;
			/*for (auto const &[key, value] : map) {*/
			for (auto const &it : map) {
				changes.push_back(typename Event::Remove(&(*it.first), &(*it.second)));
			}
			for (auto const &it : changes) {
				change.fire(it);
			}
			map.clear();
		}

		size_t size() const override {
			return map.size();
		}

		bool empty() const override {
			return map.empty();
		}
	};
}

static_assert(std::is_move_constructible<rd::ViewableMap<int, int> >::value,
			  "Is move constructible from ViewableMap<int, int>");

#endif //RD_CPP_CORE_VIEWABLE_MAP_H
