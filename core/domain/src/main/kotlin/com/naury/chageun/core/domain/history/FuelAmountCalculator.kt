package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelField

/**
 * Completes fuel amounts from any two of total, volume and unit price.
 * Every derived value is rounded half up to the nearest won or millilitre.
 */
object FuelAmountCalculator {

    fun complete(totalPriceWon: Long?, volumeMl: Long?, unitPriceWon: Long?): FuelAmounts? {
        if (listOf(totalPriceWon, volumeMl, unitPriceWon).any { it != null && it <= 0 }) return null
        return when {
            totalPriceWon != null && volumeMl != null && unitPriceWon != null ->
                FuelAmounts(totalPriceWon, volumeMl, unitPriceWon, computedField = null)
            volumeMl != null && unitPriceWon != null ->
                FuelAmounts(
                    divideRounded(volumeMl * unitPriceWon, ML_PER_LITRE),
                    volumeMl,
                    unitPriceWon,
                    FuelField.Total,
                )
            totalPriceWon != null && unitPriceWon != null ->
                FuelAmounts(
                    totalPriceWon,
                    divideRounded(totalPriceWon * ML_PER_LITRE, unitPriceWon),
                    unitPriceWon,
                    FuelField.Volume,
                )
            totalPriceWon != null && volumeMl != null ->
                FuelAmounts(
                    totalPriceWon,
                    volumeMl,
                    divideRounded(totalPriceWon * ML_PER_LITRE, volumeMl),
                    FuelField.UnitPrice,
                )
            else -> null
        }
    }

    private fun divideRounded(numerator: Long, denominator: Long): Long = (numerator + denominator / 2) / denominator

    private const val ML_PER_LITRE = 1_000L
}
