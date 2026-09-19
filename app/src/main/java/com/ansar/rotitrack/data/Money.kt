package com.ansar.rotitrack.data

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Amounts are stored as whole paise so that totals never drift the way floating-point rupees do.
 */
object Money {

    fun format(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val abs = abs(paise)
        return "%s\u20B9%d.%02d".format(sign, abs / 100, abs % 100)
    }

    /** Parses user input like "12", "12.5" or "12.50" into paise. Null if it isn't a number. */
    fun parse(text: String): Long? {
        val cleaned = text.trim().removePrefix("\u20B9").trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val rupees = cleaned.toDoubleOrNull() ?: return null
        if (rupees < 0) return null
        return (rupees * 100).roundToLong()
    }
}
