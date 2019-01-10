package com.jetbrains.rd.generator.nova
import com.jetbrains.rd.generator.nova.FlowKind.*

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
