//
// Created by jetbrains on 09.07.2018.
//

#ifndef RD_CPP_CORE_SIGNAL_H
#define RD_CPP_CORE_SIGNAL_H

#include "Lifetime.h"
#include "interfaces.h"
#include "erase_if.h"

#include <utility>
#include <functional>
#include <atomic>

namespace rd {
	extern std::atomic<int32_t> cookie;

	template<typename T>
	class Signal : public ISignal<T> {
	private:
		using WT = typename ISignal<T>::WT;

		class Event {
		private:
			std::function<void(T const &)> action;
			Lifetime lifetime;
		public:
			//region ctor/dtor
			Event() = delete;

			template<typename F>
			Event(F &&action, Lifetime lifetime) : action(std::forward<F>(action)), lifetime(std::move(lifetime)) {}

			Event(Event &&) = default;
			//endregion

			bool is_alive() const {
				return !lifetime->is_terminated();
			}

			void execute_if_alive(T const &value) const {
				if (is_alive()) {
					action(value);
				}
			}
		};

		using counter_t = int32_t;
		using listeners_t = std::map<counter_t, Event>;

		mutable counter_t advise_id = 0;
		mutable listeners_t listeners, priority_listeners;

		static void cleanup(listeners_t &queue) {
			util::erase_if(queue, [](Event const &e) -> bool { return !e.is_alive(); });
		}

		void fire_impl(T const &value, listeners_t &queue) const {
			for (auto const &p : queue) {
				auto const &event = p.second;
				event.execute_if_alive(value);
			}
			cleanup(queue);
		}

		template<typename F>
		void advise0(const Lifetime &lifetime, F &&handler, listeners_t &queue) const {
			if (lifetime->is_terminated()) return;
			counter_t id = advise_id/*.load()*/;
			queue.emplace(id, Event(std::forward<F>(handler), lifetime));
			++advise_id;
		}

	public:
		//region ctor/dtor

		Signal() = default;

		Signal(Signal const &other) = delete;

		Signal &operator=(Signal const &other) = delete;

		Signal(Signal &&) = default;

		Signal &operator=(Signal &&) = default;

		virtual ~Signal() = default;

		//endregion

		using ISignal<T>::fire;

		void fire(T const &value) const override {
			fire_impl(value, priority_listeners);
			fire_impl(value, listeners);
		}

		using ISignal<T>::advise;

		void advise(Lifetime lifetime, std::function<void(T const &)> handler) const override {
			advise0(std::move(lifetime), std::move(handler), isPriorityAdvise() ? priority_listeners : listeners);
		}

		static bool isPriorityAdvise() {
			return cookie > 0;
		}
	};

	template<typename F>
	void priorityAdviseSection(F &&block) {
		++cookie;
		block();
		--cookie;
	}
}

static_assert(std::is_move_constructible<rd::Signal<int>>::value, "Is not move constructible from Signal<int>");
static_assert(std::is_move_constructible<rd::Signal<void *>>::value, "Is not move constructible from Signal<void *>");

#endif //RD_CPP_CORE_SIGNAL_H
