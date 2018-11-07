//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASKIMPL_H
#define RD_CPP_RDTASKIMPL_H

#include <optional>

#include "Polymorphic.h"
#include "RdTaskResult.h"

template<typename T, typename S = Polymorphic<T> >
class RdTaskImpl {
    //todo friend class
public:
    Property<std::optional<RdTaskResult<T, S> > > result = Property<std::optional<RdTaskResult<T, S> > >(std::nullopt);
};


#endif //RD_CPP_RDTASKIMPL_H
