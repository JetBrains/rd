//
// Created by jetbrains on 17.10.2018.
//

#ifndef RD_CPP_OVERLOADED_H
#define RD_CPP_OVERLOADED_H

template<class... Ts>
struct overloaded : Ts ... {
    using Ts::operator()...;
};

template<class... Ts> overloaded(Ts...) -> overloaded<Ts...>;

#endif //RD_CPP_OVERLOADED_H
