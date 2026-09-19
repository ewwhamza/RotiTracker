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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansar.rotitrack.data.Money
import com.ansar.rotitrack.data.Payment
import com.ansar.rotitrack.ui.DateFormats
import com.ansar.rotitrack.ui.DueState
import com.ansar.rotitrack.ui.PickDateDialog
import com.ansar.rotitrack.ui.SummaryRow
import java.time.LocalDate

/**
 * What is owed overall: everything ordered, valued at the rate of its own day, minus what has
 * already been paid.
 */
@Composable
fun DueScreen(
    state: DueState,
    today: LocalDate,
    onAddPayment: (LocalDate, Long, String?) -> Unit,
    onDeletePayment: (Payment) -> Unit,
    modifier: Modifier = Modifier
) {
    var addingPayment by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutstandingCard(state)
        BreakdownCard(state)

        Button(
            onClick = { addingPayment = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text("  Record a payment")
        }

        PaymentsCard(state = state, onDeletePayment = onDeletePayment)
    }

    if (addingPayment) {
        AddPaymentDialog(
            today = today,
            suggestedPaise = state.outstandingPaise.coerceAtLeast(0L),
            onDismiss = { addingPayment = false },
            onConfirm = { date, paise, note ->
                addingPayment = false
                onAddPayment(date, paise, note)
            }
        )
    }
}

@Composable
private fun OutstandingCard(state: DueState) {
    val settled = state.outstandingPaise <= 0L
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (settled) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val contentColor = if (settled) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer

            Text(
                text = if (state.outstandingPaise < 0L) "Paid in advance" else "Total due",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
            Text(
                text = Money.format(
                    if (state.outstandingPaise < 0L) -state.outstandingPaise
                    else state.outstandingPaise
                ),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (state.firstDate != null && state.lastDate != null) {
                Text(
                    text = "${state.firstDate.format(DateFormats.dayShort)} to " +
                        state.lastDate.format(DateFormats.dayShort),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BreakdownCard(state: DueState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            state.lines.forEach { line ->
                SummaryRow(
                    label = "${line.item.label} × ${line.count}",
                    value = Money.format(line.valuePaise)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow(label = "Total ordered", value = Money.format(state.orderedPaise))
            SummaryRow(label = "Total paid", value = Money.format(state.paidPaise))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow(
                label = "Outstanding",
                value = Money.format(state.outstandingPaise),
                emphasise = true
            )
        }
    }
}

@Composable
private fun PaymentsCard(state: DueState, onDeletePayment: (Payment) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (state.payments.isEmpty()) {
                Text(
                    text = "Nothing paid yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                state.payments.forEach { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Money.format(payment.amountPaise),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = payment.date.format(DateFormats.dayShort) +
                                    (payment.note?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeletePayment(payment) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete this payment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPaymentDialog(
    today: LocalDate,
    suggestedPaise: Long,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Long, String?) -> Unit
) {
    var text by remember {
        mutableStateOf(if (suggestedPaise > 0) "%.2f".format(suggestedPaise / 100.0) else "")
    }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var pickingDate by remember { mutableStateOf(false) }
    val paise = Money.parse(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record a payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = text.isNotBlank() && paise == null,
                    supportingText = {
                        if (text.isNotBlank() && paise == null) Text("Enter an amount like 250")
                    }
                )
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null)
                    Text(
                        text = "  ${date.format(DateFormats.dayShort)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { paise?.let { onConfirm(date, it, note) } },
                enabled = paise != null && paise > 0L
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingDate) {
        PickDateDialog(
            initial = date,
            onDismiss = { pickingDate = false },
            onPick = { date = it }
        )
    }
}
