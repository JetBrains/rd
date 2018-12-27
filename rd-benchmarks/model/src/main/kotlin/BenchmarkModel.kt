package com.jetbrains.rd.benchmarks

import com.jetbrains.rider.generator.nova.*

object BenchmarkRootModel : Root() {
    val benchmarkInternRoot = InternRootKey("Benchmark")
    val missingInternRoot = InternRootKey("Missing")

    val InterningBenchmarkRoot = classdef {
        internRoot(benchmarkInternRoot)

        signal("notInternedInt", PredefinedType.int)
        signal("notInternedString", PredefinedType.string)
        signal("internedHereString", PredefinedType.string.interned(benchmarkInternRoot))
        signal("internedInProtocolString", PredefinedType.string.interned(ProtocolInternRoot))
        signal("internedInMissingRootString", PredefinedType.string.interned(missingInternRoot))
    }

    init {
        signal("count", PredefinedType.int)
        property("interningRoot", InterningBenchmarkRoot)
    }
}

