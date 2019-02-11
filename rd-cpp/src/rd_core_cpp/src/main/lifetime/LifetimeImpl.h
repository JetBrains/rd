#include <utility>

//
// Created by jetbrains on 09.07.2018.
//

#ifndef RD_CPP_CORE_LIFETIME_H
#define RD_CPP_CORE_LIFETIME_H

#include <functional>
#include <map>
#include <memory>
#include <mutex>
#include <atomic>

class LifetimeImpl {
private:
    friend class LifetimeDefinition;

    friend class Lifetime;

    bool eternaled = false;
    bool terminated = false;

    using counter_t = int32_t;
    counter_t id = 0;

    counter_t action_id_in_map = 0;
    using actions_t = std::map<int, std::function<void()>>;
    actions_t actions;

    void terminate();

    std::mutex lock;
public:

    //region ctor/dtor
    LifetimeImpl() = delete;

    explicit LifetimeImpl(bool is_eternal = false);

    LifetimeImpl(LifetimeImpl const &other) = delete;

    ~LifetimeImpl();
    //endregion

    template<typename F>
    counter_t add_action(F &&action) {
        std::lock_guard<decltype(lock)> guard(lock);

        if (is_eternal()) return -1;
        if (is_terminated()) throw std::invalid_argument("Already Terminated");

        actions[action_id_in_map] = std::forward<F>(action);
        return action_id_in_map++;
    }
#if __cplusplus >= 201703L
    static inline counter_t get_id = 0;
#else
	static counter_t get_id;
#endif

    template<typename F, typename G>
    void bracket(F &&opening, G &&closing) {
        if (is_terminated()) return;
        opening();
        add_action(std::forward<G>(closing));
    }

    bool is_terminated() const;

    bool is_eternal() const;

    void attach_nested(std::shared_ptr<LifetimeImpl> nested);
};

#endif //RD_CPP_CORE_LIFETIME_H
