package com.ansar.rotitrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ansar.rotitrack.RotiApp
import com.ansar.rotitrack.data.ItemType
import com.ansar.rotitrack.data.OrderEntry
import com.ansar.rotitrack.data.Payment
import com.ansar.rotitrack.data.Rate
import com.ansar.rotitrack.data.RateBook
import com.ansar.rotitrack.data.RotiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** One day's orders, with the rate each item was billed at on that day. */
data class DayState(
    val date: LocalDate,
    val entries: List<OrderEntry> = emptyList(),
    val rates: Map<ItemType, Long> = emptyMap()
) {
    fun entriesFor(item: ItemType): List<OrderEntry> = entries.filter { it.item == item }
    fun total(item: ItemType): Int = entriesFor(item).sumOf { it.count }
    fun rate(item: ItemType): Long = rates[item] ?: 0L
    fun valueOf(item: ItemType): Long = total(item).toLong() * rate(item)
    val totalPaise: Long get() = ItemType.entries.sumOf { valueOf(it) }
}

data class MonthState(
    val month: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, List<OrderEntry>> = emptyMap(),
    val totals: Map<ItemType, Int> = emptyMap(),
    val valuePaise: Long = 0L
) {
    fun entriesOn(date: LocalDate): List<OrderEntry> = entries[date].orEmpty()
    fun countOn(date: LocalDate, item: ItemType): Int =
        entriesOn(date).filter { it.item == item }.sumOf { it.count }
    fun totalOn(date: LocalDate): Int = entriesOn(date).sumOf { it.count }
    fun total(item: ItemType): Int = totals[item] ?: 0
    val daysOrdered: Int get() = entries.count { (_, list) -> list.any { it.count > 0 } }
}

data class ItemLine(val item: ItemType, val count: Int, val valuePaise: Long)

data class DueState(
    val lines: List<ItemLine> = emptyList(),
    val orderedPaise: Long = 0L,
    val paidPaise: Long = 0L,
    val payments: List<Payment> = emptyList(),
    val firstDate: LocalDate? = null,
    val lastDate: LocalDate? = null
) {
    val outstandingPaise: Long get() = orderedPaise - paidPaise
}

data class RatesState(
    val today: LocalDate = LocalDate.now(),
    /** Null means no rate has ever been set for that item. */
    val current: Map<ItemType, Long?> = emptyMap(),
    val history: Map<ItemType, List<Rate>> = emptyMap()
) {
    fun historyFor(item: ItemType): List<Rate> = history[item].orEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RotiViewModel(private val repo: RotiRepository) : ViewModel() {

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    /** Called when the app comes back to the foreground, so the home screen rolls over at midnight. */
    fun refreshToday() {
        _today.value = LocalDate.now()
    }

    val rateBook: StateFlow<RateBook> = repo.rateBook
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RateBook(emptyList()))

    val homeDay: StateFlow<DayState> = _today
        .flatMapLatest { date ->
            combine(repo.entriesOn(date), repo.rateBook) { entries, book ->
                DayState(
                    date = date,
                    entries = entries,
                    rates = ItemType.entries.associateWith { book.priceOn(it, date) }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayState(LocalDate.now()))

    val monthState: StateFlow<MonthState> = _month
        .flatMapLatest { ym ->
            combine(
                repo.entriesBetween(ym.atDay(1), ym.atEndOfMonth()),
                repo.rateBook
            ) { entries, book ->
                MonthState(
                    month = ym,
                    entries = entries.groupBy { it.date },
                    totals = ItemType.entries.associateWith { item ->
                        entries.filter { it.item == item }.sumOf { it.count }
                    },
                    valuePaise = book.valueOf(entries)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthState())

    val dueState: StateFlow<DueState> =
        combine(repo.allEntries, repo.rateBook, repo.payments) { entries, book, payments ->
            val lines = ItemType.entries.map { item ->
                val rows = entries.filter { it.item == item }
                ItemLine(item, rows.sumOf { it.count }, book.valueOf(rows))
            }
            DueState(
                lines = lines,
                orderedPaise = lines.sumOf { it.valuePaise },
                paidPaise = payments.sumOf { it.amountPaise },
                payments = payments,
                firstDate = entries.minOfOrNull { it.date },
                lastDate = entries.maxOfOrNull { it.date }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DueState())

    val ratesState: StateFlow<RatesState> =
        combine(_today, repo.rateBook) { today, book ->
            RatesState(
                today = today,
                current = ItemType.entries.associateWith { item ->
                    if (book.hasAnyRate(item)) book.priceOn(item, today) else null
                },
                history = ItemType.entries.associateWith { book.historyFor(it) }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RatesState())

    fun addEntry(date: LocalDate, item: ItemType, count: Int) {
        viewModelScope.launch { repo.addEntry(date, item, count) }
    }

    fun deleteEntry(entry: OrderEntry) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }

    fun showMonth(month: YearMonth) {
        _month.value = month
    }

    fun shiftMonth(months: Long) {
        _month.value = _month.value.plusMonths(months)
    }

    fun setRate(item: ItemType, pricePaise: Long, effectiveFrom: LocalDate) {
        viewModelScope.launch { repo.setRate(item, pricePaise, effectiveFrom) }
    }

    fun deleteRate(rate: Rate) {
        viewModelScope.launch { repo.deleteRate(rate) }
    }

    fun addPayment(date: LocalDate, amountPaise: Long, note: String?) {
        viewModelScope.launch { repo.addPayment(date, amountPaise, note) }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch { repo.deletePayment(payment) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RotiApp
                RotiViewModel(app.repository)
            }
        }
    }
}
