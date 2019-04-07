//
// Created by jetbrains on 4/7/2019.
//

#include "SynchronousScheduler.h"

#include "guards.h"

namespace rd {
	thread_local int32_t SynchronousScheduler::active = 0;

	SynchronousScheduler globalSynchronousScheduler;
}