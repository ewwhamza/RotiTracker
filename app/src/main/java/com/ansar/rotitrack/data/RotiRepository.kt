package com.ansar.rotitrack.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

class RotiRepository(private val dao: RotiDao) {

    val rates: Flow<List<Rate>> = dao.allRates()
    val rateBook: Flow<RateBook> = rates.map(::RateBook)
    val payments: Flow<List<Payment>> = dao.allPayments()
    val allEntries: Flow<List<OrderEntry>> = dao.allEntries()

    fun entriesOn(date: LocalDate): Flow<List<OrderEntry>> = dao.entriesOn(date)

    fun entriesBetween(start: LocalDate, end: LocalDate): Flow<List<OrderEntry>> =
        dao.entriesBetween(start, end)

    /** Records one order. Adding 20 then 10 leaves two rows and a day total of 30. */
    suspend fun addEntry(date: LocalDate, item: ItemType, count: Int, at: LocalTime = LocalTime.now()) {
        if (count <= 0) return
        dao.insertEntry(OrderEntry(date = date, item = item, count = count, recordedAt = at))
    }

    suspend fun deleteEntry(entry: OrderEntry) = dao.deleteEntry(entry)

    suspend fun setRate(item: ItemType, pricePaise: Long, effectiveFrom: LocalDate) {
        dao.insertRate(Rate(item = item, pricePaise = pricePaise, effectiveFrom = effectiveFrom))
    }

    suspend fun deleteRate(rate: Rate) = dao.deleteRate(rate)

    suspend fun addPayment(date: LocalDate, amountPaise: Long, note: String?) {
        dao.insertPayment(Payment(date = date, amountPaise = amountPaise, note = note?.ifBlank { null }))
    }

    suspend fun deletePayment(payment: Payment) = dao.deletePayment(payment)
}
