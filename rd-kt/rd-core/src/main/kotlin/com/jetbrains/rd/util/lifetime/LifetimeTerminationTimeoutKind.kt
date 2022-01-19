package com.jetbrains.rd.util.lifetime

/**
 * Lifetime's termination timeout kind. The actual milliseconds value can be assigned via [Lifetime.setTerminationTimeoutMs].
 */
enum class LifetimeTerminationTimeoutKind(val value: Int) {
    /**
     * Default timeout (500ms).
     *
     * The actual value defined by <see [Lifetime.waitForExecutingInTerminationTimeoutMs] (compatibility mode).
     */
    Default(-1),
    /**
     * Short timeout (250ms).
     *
     * The actual value can be overridden via [Lifetime.setTerminationTimeoutMs].
     */
    Short(1),
    /**
     * Short timeout (5s).
     *
     * The actual value can be overridden via [Lifetime.setTerminationTimeoutMs].
     */
    Long(2),
    /**
     * Short timeout (30s).
     *
     * The actual value can be overridden via [Lifetime.setTerminationTimeoutMs].
     */
    ExtraLong(3);

    companion object {
        val maxValue get() = ExtraLong
    }
}