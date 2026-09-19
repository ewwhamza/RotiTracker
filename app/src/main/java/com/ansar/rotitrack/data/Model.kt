package com.ansar.rotitrack.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/** The things that can be ordered. Each has its own daily count and its own rate history. */
enum class ItemType(val label: String) {
    ROTI("Roti"),
    CHAPATI("Chapati")
}

/**
 * One order placed at one moment: 20 roti at lunch is one entry, 10 more at dinner is another.
 * A day's count is the sum of its entries, which keeps the two orders separately visible and
 * separately deletable.
 */
@Entity(tableName = "order_entry", indices = [Index(value = ["date"])])
data class OrderEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val item: ItemType,
    val count: Int,
    /** Wall-clock time the order was added, so lunch and dinner read apart in the day's list. */
    val recordedAt: LocalTime
)

/**
 * The price of one [item] from [effectiveFrom] onwards, in paise. Changing a rate adds a new
 * row rather than editing the old one, so days already logged keep the price they were billed at.
 */
@Entity(
    tableName = "rate",
    indices = [Index(value = ["item", "effectiveFrom"], unique = true)]
)
data class Rate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: ItemType,
    val pricePaise: Long,
    val effectiveFrom: LocalDate
)

/** Money handed over to the vendor. Subtracted from the value of everything ordered. */
@Entity(tableName = "payment")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val amountPaise: Long,
    val note: String? = null
)

class Converters {
    @TypeConverter fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
    @TypeConverter fun epochDayToDate(day: Long?): LocalDate? = day?.let(LocalDate::ofEpochDay)
    @TypeConverter fun timeToSecondOfDay(time: LocalTime?): Int? = time?.toSecondOfDay()
    @TypeConverter fun secondOfDayToTime(second: Int?): LocalTime? =
        second?.let { LocalTime.ofSecondOfDay(it.toLong()) }
    @TypeConverter fun itemToName(item: ItemType?): String? = item?.name
    @TypeConverter fun nameToItem(name: String?): ItemType? = name?.let(ItemType::valueOf)
}
