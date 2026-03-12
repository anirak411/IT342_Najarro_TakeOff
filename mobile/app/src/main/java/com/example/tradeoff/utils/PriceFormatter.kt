package com.example.tradeoff.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PriceFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US)

    fun format(value: Double): String {
        val formatter = DecimalFormat("#,##0.00", symbols)
        formatter.isGroupingUsed = true
        return formatter.format(value)
    }

    fun parse(input: String): Double? {
        val normalized = input.replace(",", "").trim()
        return normalized.toDoubleOrNull()
    }
}
