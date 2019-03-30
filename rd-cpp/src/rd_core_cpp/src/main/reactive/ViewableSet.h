//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLESET_H
#define RD_CPP_CORE_VIEWABLESET_H

#include "IViewableSet.h"
#include "SignalX.h"
#include "util/core_util.h"

#include "tsl/ordered_set.h"


namespace rd {
	template<typename T>
	class ViewableSet : public IViewableSet<T> {
	public:
		using Event = typename IViewableSet<T>::Event;

		using IViewableSet<T>::advise;
	private:
		using WT = typename IViewableSet<T>::WT;

		Signal<Event> change;
		using data_t = tsl::ordered_set<Wrapper<T>, wrapper::TransparentHash<T>, wrapper::TransparentKeyEqual<T>>;
		mutable data_t set;
	public:
		//region ctor/dtor

		ViewableSet() = default;

		ViewableSet(ViewableSet &&) = default;

		ViewableSet &operator=(ViewableSet &&) = default;

		virtual ~ViewableSet() = default;
		//endregion

		//region iterators

	protected:
		using iterator_trait = std::iterator<
				std::bidirectional_iterator_tag,
				T,
				std::ptrdiff_t,
				const T *,
				const T &>;
	public:
		class iterator : public iterator_trait {
			friend class ViewableSet<T>;

			typename data_t::iterator it_;

			explicit iterator(const typename data_t::iterator &it) : it_(it) {}

		public:
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

			iterator &operator+=(typename iterator_trait::difference_type delta) {
				it_ += delta;
				return *this;
			}

			iterator &operator-=(typename iterator_trait::difference_type delta) {
				it_ -= delta;
				return *this;
			}

			iterator operator+(typename iterator_trait::difference_type delta) const {
				auto it = *this;
				return it += delta;
			}

			iterator operator-(typename iterator_trait::difference_type delta) const {
				auto it = *this;
				return it -= delta;
			}

			typename iterator_trait::difference_type operator-(iterator const &other) const {
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

			typename iterator_trait::reference operator*() const noexcept {
				return **it_;
			}

			typename iterator_trait::pointer operator->() const noexcept {
				return (*it_).get();
			}
		};

		using reverse_iterator = std::reverse_iterator<iterator>;

		using const_iterator = iterator;

		using const_reverse_iterator = reverse_iterator;

		iterator begin() { return iterator(set.begin()); }

		const_iterator begin() const { return const_iterator(set.begin()); }

		iterator end() { return iterator(set.end()); }

		const_iterator end() const { return const_iterator(set.end()); }

		reverse_iterator rbegin() { return reverse_iterator(end()); }

		const_reverse_iterator rbegin() const { return const_reverse_iterator(end()); }

		reverse_iterator rend() { return reverse_iterator(begin()); }

		const_reverse_iterator rend() const { return const_reverse_iterator(begin()); }
		//endregion

		bool add(WT element) const override {
			/*auto const &[it, success] = set.emplace(std::make_unique<T>(std::move(element)));*/
			auto const &it = set.emplace(std::move(element));
			if (!it.second) {
				return false;
			}
			change.fire(Event(AddRemove::ADD, &(wrapper::get<T>(*it.first))));
			return true;
		}

		bool addAll(std::vector<WT> elements) const override {
			for (auto &&element : elements) {
				add(std::move(element));
			}
			return true;
		}

		void clear() const override {
			std::vector<Event> changes;
			for (auto const &element : set) {
				changes.push_back(Event(AddRemove::REMOVE, &(*element)));
			}
			for (auto const &e : changes) {
				change.fire(e);
			}
			set.clear();
		}

		bool remove(T const &element) const override {
			if (!contains(element)) {
				return false;
			}
			auto it = set.find(element);
			change.fire(Event(AddRemove::REMOVE, &(wrapper::get<T>(*it))));
			set.erase(it);
			return true;
		}

		void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
			for (auto const &x : set) {
				handler(Event(AddRemove::ADD, &(*x)));
			}
			change.advise(lifetime, handler);
		}

		size_t size() const override {
			return set.size();
		}

		bool contains(T const &element) const override {
			return set.count(element) > 0;
		}

		bool empty() const override {
			return set.empty();
		}
	};
}

static_assert(std::is_move_constructible<rd::ViewableSet<int> >::value, "Is move constructible from ViewableSet<int>");

#endif //RD_CPP_CORE_VIEWABLESET_H
