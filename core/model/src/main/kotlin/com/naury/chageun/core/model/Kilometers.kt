package com.naury.chageun.core.model

@JvmInline
value class Kilometers(val value: Long) : Comparable<Kilometers> {
    init {
        require(value >= 0) { "Odometer value cannot be negative" }
    }

    override fun compareTo(other: Kilometers): Int = value.compareTo(other.value)

    operator fun plus(other: Kilometers) = Kilometers(value + other.value)

    /** Signed difference; negative when [other] is ahead of this reading. */
    infix fun distanceFrom(other: Kilometers): Long = value - other.value
}
