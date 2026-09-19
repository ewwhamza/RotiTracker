@file:OptIn(ExperimentalMaterial3Api::class)

package com.ansar.rotitrack.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansar.rotitrack.data.ItemType
import com.ansar.rotitrack.data.OrderEntry
import com.ansar.rotitrack.ui.AnimatedMoney
import com.ansar.rotitrack.ui.AppearAnimated
import com.ansar.rotitrack.ui.DayState
import com.ansar.rotitrack.ui.OrderComposerCard
import com.ansar.rotitrack.ui.OrderEntryRow
import com.ansar.rotitrack.ui.RollingNumber
import kotlinx.coroutines.launch

/**
 * Home screen. The counter holds the order being entered right now; Add commits it to the day.
 * Ordering 20 at lunch and 10 at dinner leaves the counter at 0 and the day total at 30.
 */
@Composable
fun OrderScreen(
    day: DayState,
    onAddEntry: (ItemType, Int) -> Unit,
    onDeleteEntry: (OrderEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingRoti by rememberSaveable { mutableIntStateOf(0) }
    var pendingChapati by rememberSaveable { mutableIntStateOf(0) }
    var chapatiSheetOpen by rememberSaveable { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TodaySummary(day)

        OrderComposerCard(
            title = "Ordering ${ItemType.ROTI.label}",
            pending = pendingRoti,
            ratePaise = day.rate(ItemType.ROTI),
            onPendingChange = { pendingRoti = it.coerceAtLeast(0) },
            onAdd = {
                onAddEntry(ItemType.ROTI, pendingRoti)
                pendingRoti = 0
            }
        )

        OutlinedButton(
            onClick = { chapatiSheetOpen = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text("  Add ${ItemType.CHAPATI.label}")
        }

        TodayOrders(day = day, onDeleteEntry = onDeleteEntry)
    }

    if (chapatiSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { chapatiSheetOpen = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                OrderComposerCard(
                    title = "Ordering ${ItemType.CHAPATI.label}",
                    pending = pendingChapati,
                    ratePaise = day.rate(ItemType.CHAPATI),
                    onPendingChange = { pendingChapati = it.coerceAtLeast(0) },
                    onAdd = {
                        onAddEntry(ItemType.CHAPATI, pendingChapati)
                        pendingChapati = 0
                        // Let the number settle visibly before the sheet slides away.
                        scope.launch {
                            sheetState.hide()
                            chapatiSheetOpen = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TodaySummary(day: DayState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ItemType.entries.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ordered ${item.label} Today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    RollingNumber(
                        value = day.total(item),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today costs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                AnimatedMoney(
                    paise = day.totalPaise,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** The day's orders in the order they were placed, so lunch and dinner stay distinguishable. */
@Composable
private fun TodayOrders(day: DayState, onDeleteEntry: (OrderEntry) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's orders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (day.entries.isEmpty()) {
                Text(
                    text = "Nothing ordered yet. Set a number above and tap Add to today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                day.entries.forEach { entry ->
                    key(entry.id) {
                        AppearAnimated {
                            OrderEntryRow(
                                entry = entry,
                                ratePaise = day.rate(entry.item),
                                onDelete = { onDeleteEntry(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}
