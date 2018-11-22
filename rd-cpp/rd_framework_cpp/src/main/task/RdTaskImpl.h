//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASKIMPL_H
#define RD_CPP_RDTASKIMPL_H

#include "Polymorphic.h"
#include "RdTaskResult.h"

#include "optional.hpp"


template<typename T, typename S = Polymorphic<T> >
class RdTaskImpl {
    //todo friend class
public:
    Property<tl::optional<RdTaskResult<T, S> > > result{tl::nullopt};
};


#endif //RD_CPP_RDTASKIMPL_H
