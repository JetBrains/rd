package com.jetbrains.rd.benchmarks

import com.jetbrains.rider.generator.nova.*

object BenchmarkRootModel : Root() {
    init {
        property("count", PredefinedType.int)
    }
}

