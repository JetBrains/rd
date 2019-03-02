//
// Created by jetbrains on 3/1/2019.
//

#ifndef RD_CPP_INTERNSCHEDULER_H
#define RD_CPP_INTERNSCHEDULER_H

#include "IScheduler.h"

namespace rd {

    class InternScheduler : public IScheduler {
    public:
        //region ctor/dtor

        InternScheduler();
        //endregion

        void queue(std::function<void()> action) override;

        void flush() override;

        bool is_active() const override;
    };
}


#endif //RD_CPP_INTERNSCHEDULER_H
