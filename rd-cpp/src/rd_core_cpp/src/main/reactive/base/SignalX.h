#ifndef RD_CPP_CORE_SIGNAL_H
#define RD_CPP_CORE_SIGNAL_H

#include "interfaces.h"
#include "SignalCookie.h"
#include "lifetime/LifetimeDefinition.h"

#include <lifetime/Lifetime.h>
#include <util/core_util.h>

#include <utility>
#include <functional>
#include <atomic>
#include <list>

namespace rd
{
/**
 * \brief complete class which has \a Signal<T> 's properties
 */
template <typename T>
class Signal final : public ISignal<T>
{
private:
	using WT = typename ISignal<T>::WT;

	struct Event
	{
		using F = std::function<void(T const&)>;
	private:
		Lifetime lifetime;
		F action;
		std::atomic_int8_t state;

		constexpr static int8_t ACTIVE = 0;
		constexpr static int8_t FIRING = 1;
		constexpr static int8_t TERMINATED = 2;
	public:
		// region ctor/dtor
		Event() = delete;
		explicit Event(const Lifetime& lifetime, F&& action) : lifetime(lifetime), action(std::forward<F>(action)), state(ACTIVE)
		{
		}

		Event(Event&&) = default;
		Event& operator=(Event&& other) = default;

		bool operator()(T const& arg)
		{
			if (lifetime->is_terminated())
				return false;

			auto expected_state = ACTIVE;
			// set firing flag to prevent action destruction during action firing
			// skip action if it isn't active (lifetime was terminated)
			if (!state.compare_exchange_strong(expected_state, FIRING))
				return false;

			expected_state = FIRING;
			try
			{
				action(arg);
				return state.compare_exchange_strong(expected_state, ACTIVE);
			}
			catch (...)
			{
				if (!state.compare_exchange_strong(expected_state, ACTIVE))
					action = nullptr;
				throw;
			}
		}

		void terminate()
		{
			const auto old_state = state.exchange(TERMINATED);
			// release action immediatelly if it isn't firing right now
			if (old_state == ACTIVE)
				action = nullptr;
			lifetime = Lifetime::Terminated();
		}
	};

	using listeners_t = std::vector<std::shared_ptr<Event>>;

	mutable listeners_t listeners, priority_listeners;

	void fire_impl(T const& value, listeners_t& queue) const
	{
		auto it = queue.begin();
		auto end = queue.end();
		auto alive_it = it;
		while (it != end)
		{
			if (it->get()->operator()(value))
			{
				if (alive_it != it)
					*alive_it = std::move(*it);
				++alive_it;
			}
			++it;
		}
		if (alive_it != end)
			queue.erase(alive_it, end);
	}

	template <typename F>
	void advise0(const Lifetime& lifetime, F&& handler, listeners_t& queue) const
	{
		if (lifetime->is_terminated())
			return;
		auto event_ptr = std::make_shared<Event>(lifetime, std::forward<F>(handler));
		lifetime->add_action([event_ptr] { event_ptr->terminate(); });
		queue.push_back(std::move(event_ptr));
	}

public:
	// region ctor/dtor

	Signal() = default;

	Signal(Signal const& other) = delete;

	Signal& operator=(Signal const& other) = delete;

	Signal(Signal&&) = default;

	Signal& operator=(Signal&&) = default;

	virtual ~Signal() = default;

	// endregion

	using ISignal<T>::fire;

	void fire(T const& value) const override
	{
		fire_impl(value, priority_listeners);
		fire_impl(value, listeners);
	}

	using ISignal<T>::advise;

	void advise(Lifetime lifetime, std::function<void(T const&)> handler) const override
	{
		advise0(lifetime, std::move(handler), isPriorityAdvise() ? priority_listeners : listeners);
	}

	static bool isPriorityAdvise()
	{
		return rd_signal_cookie_get() > 0;
	}
};

template <typename F>
void priorityAdviseSection(F&& block)
{
	rd_signal_cookie_inc();
	block();
	rd_signal_cookie_dec();
}
}	 // namespace rd

static_assert(std::is_move_constructible<rd::Signal<int>>::value, "Is not move constructible from Signal<int>");
static_assert(std::is_move_constructible<rd::Signal<rd::Void>>::value, "Is not move constructible from Signal<Void>");

#endif	  // RD_CPP_CORE_SIGNAL_H
