package com.ansar.rotitrack.data

import java.time.LocalDate

/**
 * Answers "what did one roti cost on this day?" from the full rate history.
 *
 * A day is billed at the newest rate that was already in effect on that day. Days logged before
 * any rate existed fall back to the earliest rate on record, so backdated entries are never
 * silently valued at zero.
 */
class RateBook(rates: List<Rate>) {

    private val history: Map<ItemType, List<Rate>> = ItemType.entries.associateWith { item ->
        rates.filter { it.item == item }.sortedWith(compareBy({ it.effectiveFrom }, { it.id }))
    }

    fun priceOn(item: ItemType, date: LocalDate): Long {
        val forItem = history[item].orEmpty()
        return forItem.lastOrNull { it.effectiveFrom <= date }?.pricePaise
            ?: forItem.firstOrNull()?.pricePaise
            ?: 0L
    }

    /** Newest first, which is the order the rates screen lists them in. */
    fun historyFor(item: ItemType): List<Rate> = history[item].orEmpty().reversed()

    fun hasAnyRate(item: ItemType): Boolean = history[item].orEmpty().isNotEmpty()

    fun valueOf(entries: List<OrderEntry>): Long =
        entries.sumOf { it.count.toLong() * priceOn(it.item, it.date) }
}
