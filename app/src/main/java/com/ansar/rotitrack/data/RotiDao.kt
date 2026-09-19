package com.ansar.rotitrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RotiDao {

    @Query("SELECT * FROM order_entry WHERE date = :date ORDER BY recordedAt, id")
    fun entriesOn(date: LocalDate): Flow<List<OrderEntry>>

    @Query("SELECT * FROM order_entry WHERE date BETWEEN :start AND :end ORDER BY date, recordedAt, id")
    fun entriesBetween(start: LocalDate, end: LocalDate): Flow<List<OrderEntry>>

    @Query("SELECT * FROM order_entry ORDER BY date, recordedAt, id")
    fun allEntries(): Flow<List<OrderEntry>>

    @Insert
    suspend fun insertEntry(entry: OrderEntry)

    @Delete
    suspend fun deleteEntry(entry: OrderEntry)

    @Query("SELECT * FROM rate ORDER BY effectiveFrom, id")
    fun allRates(): Flow<List<Rate>>

    /** REPLACE so that re-setting the rate for a date already covered overwrites it. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: Rate)

    @Delete
    suspend fun deleteRate(rate: Rate)

    @Query("SELECT * FROM payment ORDER BY date DESC, id DESC")
    fun allPayments(): Flow<List<Payment>>

    @Insert
    suspend fun insertPayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)
}
