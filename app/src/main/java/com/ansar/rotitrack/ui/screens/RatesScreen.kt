package com.ansar.rotitrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ansar.rotitrack.data.ItemType
import com.ansar.rotitrack.data.Money
import com.ansar.rotitrack.data.Rate
import com.ansar.rotitrack.ui.DateFormats
import com.ansar.rotitrack.ui.PickDateDialog
import com.ansar.rotitrack.ui.RatesState
import java.time.LocalDate

/**
 * Rates are kept as history rather than a single editable number, so raising the price today
 * does not re-price every roti already logged.
 */
@Composable
fun RatesScreen(
    state: RatesState,
    onSetRate: (ItemType, Long, LocalDate) -> Unit,
    onDeleteRate: (Rate) -> Unit,
    modifier: Modifier = Modifier
) {
    var changing by remember { mutableStateOf<ItemType?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ItemType.entries.forEach { item ->
            RateCard(
                item = item,
                currentPaise = state.current[item],
                history = state.historyFor(item),
                onChangeClick = { changing = item },
                onDeleteRate = onDeleteRate
            )
        }

        Text(
            text = "A new rate applies from its start date onwards. Days already logged keep the " +
                "rate that was in effect when they happened.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    changing?.let { item ->
        SetRateDialog(
            item = item,
            initialPaise = state.current[item] ?: 0L,
            today = state.today,
            onDismiss = { changing = null },
            onConfirm = { paise, from ->
                changing = null
                onSetRate(item, paise, from)
            }
        )
    }
}

@Composable
private fun RateCard(
    item: ItemType,
    currentPaise: Long?,
    history: List<Rate>,
    onChangeClick: () -> Unit,
    onDeleteRate: (Rate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentPaise?.let { "${Money.format(it)} each" } ?: "No rate set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onChangeClick) { Text("Change") }
            }

            if (history.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                history.forEach { rate ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Money.format(rate.pricePaise)} from " +
                                rate.effectiveFrom.format(DateFormats.dayShort),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { onDeleteRate(rate) },
                            enabled = history.size > 1
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete this rate"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRateDialog(
    item: ItemType,
    initialPaise: Long,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (Long, LocalDate) -> Unit
) {
    var text by remember {
        mutableStateOf(if (initialPaise > 0) "%.2f".format(initialPaise / 100.0) else "")
    }
    var effectiveFrom by remember { mutableStateOf(today) }
    var pickingDate by remember { mutableStateOf(false) }
    val paise = Money.parse(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.label} rate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Price per ${item.label.lowercase()}") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = text.isNotBlank() && paise == null,
                    supportingText = {
                        if (text.isNotBlank() && paise == null) Text("Enter an amount like 12.50")
                    }
                )
                OutlinedButton(
                    onClick = { pickingDate = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null)
                    Text(
                        text = "  From ${effectiveFrom.format(DateFormats.dayShort)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { paise?.let { onConfirm(it, effectiveFrom) } },
                enabled = paise != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingDate) {
        PickDateDialog(
            initial = effectiveFrom,
            onDismiss = { pickingDate = false },
            onPick = { effectiveFrom = it }
        )
    }
}
