package com.naury.chageun.core.model

@JvmInline
value class Kilometers(val value: Long) : Comparable<Kilometers> {
    init {
        require(value >= 0) { "Odometer value cannot be negative" }
    }

    override fun compareTo(other: Kilometers): Int = value.compareTo(other.value)

    operator fun plus(other: Kilometers) = Kilometers(value + other.value)

    /** 부호가 있는 차이다. [other]가 이 값보다 크면 음수다. */
    infix fun distanceFrom(other: Kilometers): Long = value - other.value
}
