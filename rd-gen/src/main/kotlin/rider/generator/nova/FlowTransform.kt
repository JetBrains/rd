package com.jetbrains.rider.generator.nova
import com.jetbrains.rider.generator.nova.FlowKind.*

enum class FlowTransform {
    AsIs  {
        override fun transform(flow: FlowKind) = flow
    },
    Reversed {
        override fun transform(flow: FlowKind) = when (flow) {
            Source -> Sink
            Sink -> Source
            Both -> Both
        }
    },
    Symmetric {
        override fun transform(flow: FlowKind) = Both
    };

    abstract fun transform(flow: FlowKind) : FlowKind
}
