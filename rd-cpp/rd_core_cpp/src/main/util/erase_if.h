//
// Created by jetbrains on 25.10.2018.
//

#ifndef RD_CPP_ERASE_IF_H
#define RD_CPP_ERASE_IF_H

template<typename ContainerT, class _Pr>
void erase_if(ContainerT &items, _Pr _Pred) {
    for (auto it = items.begin(); it != items.end();) {
        if (_Pred(it->second)) it = items.erase(it);
        else ++it;
    }
}

#endif //RD_CPP_ERASE_IF_H
