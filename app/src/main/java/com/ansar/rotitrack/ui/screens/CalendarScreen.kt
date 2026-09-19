@file:OptIn(ExperimentalMaterial3Api::class)

package com.ansar.rotitrack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ansar.rotitrack.data.ItemType
import com.ansar.rotitrack.data.Money
import com.ansar.rotitrack.data.OrderEntry
import com.ansar.rotitrack.data.RateBook
import com.ansar.rotitrack.ui.AppearAnimated
import com.ansar.rotitrack.ui.CompactComposer
import com.ansar.rotitrack.ui.DateFormats
import com.ansar.rotitrack.ui.MonthState
import com.ansar.rotitrack.ui.OrderEntryRow
import com.ansar.rotitrack.ui.RollingNumber
import com.ansar.rotitrack.ui.SummaryRow
import java.time.LocalDate

/**
 * Month grid. Each day shows how many were ordered; tapping a day opens its orders, which is how
 * a missed entry gets filled in after the fact.
 */
@Composable
fun CalendarScreen(
    state: MonthState,
    today: LocalDate,
    rateBook: RateBook,
    onShiftMonth: (Long) -> Unit,
    onAddEntry: (LocalDate, ItemType, Int) -> Unit,
    onDeleteEntry: (OrderEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MonthHeader(month = state, onShiftMonth = onShiftMonth)

        WeekdayHeader()

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                val forward = targetState.month > initialState.month
                val enter = slideInHorizontally { w -> if (forward) w else -w } + fadeIn()
                val exit = slideOutHorizontally { w -> if (forward) -w else w } + fadeOut()
                enter togetherWith exit
            },
            label = "month-grid"
        ) { shown ->
            MonthGrid(state = shown, today = today, onDayClick = { editing = it })
        }

        MonthSummary(state)
    }

    editing?.let { date ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = sheetState
        ) {
            DayEditorSheet(
                date = date,
                entries = state.entriesOn(date),
                rateBook = rateBook,
                onAddEntry = { item, count -> onAddEntry(date, item, count) },
                onDeleteEntry = onDeleteEntry
            )
        }
    }
}

@Composable
private fun MonthHeader(month: MonthState, onShiftMonth: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(onClick = { onShiftMonth(-1) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        AnimatedContent(
            targetState = month.month,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally { w -> if (forward) w / 2 else -w / 2 } + fadeIn()
                val exit = slideOutHorizontally { w -> if (forward) -w / 2 else w / 2 } + fadeOut()
                enter togetherWith exit
            },
            label = "month-title"
        ) { shown ->
            Text(
                text = shown.atDay(1).format(DateFormats.monthTitle),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        FilledTonalIconButton(onClick = { onShiftMonth(1) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

private val WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAYS.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    state: MonthState,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val firstOfMonth = state.month.atDay(1)
    // DayOfWeek is Monday=1..Sunday=7; the grid starts on Sunday, so Sunday maps to 0 blanks.
    val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = state.month.lengthOfMonth()
    val rows = (leadingBlanks + daysInMonth + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { column ->
                    val dayOfMonth = row * 7 + column - leadingBlanks + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = state.month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            total = state.totalOn(date),
                            isToday = date == today,
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(date) }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    total: Int,
    isToday: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val hasOrder = total > 0
    val background = if (hasOrder) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (hasOrder) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .background(background, RoundedCornerShape(8.dp))
            .then(
                if (isToday) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = foreground
            )
            Text(
                text = if (hasOrder) total.toString() else "·",
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasOrder) foreground else Color.Transparent
            )
        }
    }
}

@Composable
private fun MonthSummary(state: MonthState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "This month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ItemType.entries.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.label, style = MaterialTheme.typography.bodyLarge)
                    RollingNumber(
                        value = state.total(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            SummaryRow(label = "Days ordered", value = state.daysOrdered.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow(
                label = "Month total",
                value = Money.format(state.valuePaise),
                emphasise = true
            )
        }
    }
}

@Composable
private fun DayEditorSheet(
    date: LocalDate,
    entries: List<OrderEntry>,
    rateBook: RateBook,
    onAddEntry: (ItemType, Int) -> Unit,
    onDeleteEntry: (OrderEntry) -> Unit
) {
    val dayValue = entries.sumOf { it.count.toLong() * rateBook.priceOn(it.item, it.date) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = date.format(DateFormats.dayWithName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        ItemType.entries.forEach { item ->
            var pending by remember(item) { mutableIntStateOf(0) }
            val itemEntries = entries.filter { it.item == item }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    RollingNumber(
                        value = itemEntries.sumOf { it.count },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                CompactComposer(
                    label = item.label,
                    pending = pending,
                    onPendingChange = { pending = it.coerceAtLeast(0) },
                    onAdd = {
                        onAddEntry(item, pending)
                        pending = 0
                    }
                )

                itemEntries.forEach { entry ->
                    key(entry.id) {
                        AppearAnimated {
                            OrderEntryRow(
                                entry = entry,
                                ratePaise = rateBook.priceOn(entry.item, entry.date),
                                onDelete = { onDeleteEntry(entry) }
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
        }

        SummaryRow(label = "Day total", value = Money.format(dayValue), emphasise = true)
    }
}
