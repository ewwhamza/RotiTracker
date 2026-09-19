@file:OptIn(ExperimentalMaterial3Api::class)

package com.ansar.rotitrack.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansar.rotitrack.data.Money
import com.ansar.rotitrack.data.OrderEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormats {
    /** Matches the header in the design: 19-09-2026 (Saturday) */
    val dayWithName: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy (EEEE)", Locale.getDefault())
    val dayShort: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    val monthTitle: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val timeOfDay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
}

/**
 * A number that rolls up or down when it changes, so a total moving from 20 to 30 reads as a
 * change rather than a redraw.
 */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    textAlign: TextAlign? = null
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val goingUp = targetState > initialState
            val enter = slideInVertically { height -> if (goingUp) height else -height } + fadeIn()
            val exit = slideOutVertically { height -> if (goingUp) -height else height } + fadeOut()
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        label = "rolling-number",
        modifier = modifier
    ) { shown ->
        Text(
            text = shown.toString(),
            style = style,
            fontWeight = fontWeight,
            color = color,
            textAlign = textAlign,
            modifier = if (textAlign != null) Modifier.fillMaxWidth() else Modifier
        )
    }
}

/** Money that crossfades on change, for totals that update as orders are added. */
@Composable
fun AnimatedMoney(
    paise: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    AnimatedContent(
        targetState = paise,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "animated-money",
        modifier = modifier
    ) { shown ->
        Text(text = Money.format(shown), style = style, fontWeight = fontWeight, color = color)
    }
}

/** Fades and expands its content in on first appearance, used for newly added order rows. */
@Composable
fun AppearAnimated(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(tween(140)) + shrinkVertically()
    ) {
        content()
    }
}

/**
 * The stepper from the design, now an order composer: the number is the order being entered,
 * and Add commits it to the day. It never shows the day's running total.
 */
@Composable
fun OrderComposerCard(
    title: String,
    pending: Int,
    ratePaise: Long,
    onPendingChange: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var typing by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close $title")
                    }
                }
            }

            StepperButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Add one to $title",
                enabled = true,
                onClick = { onPendingChange(pending + 1) }
            )

            Surface(
                modifier = Modifier
                    .widthIn(min = 140.dp)
                    .clickable { typing = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                RollingNumber(
                    value = pending,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            StepperButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Remove one from $title",
                enabled = pending > 0,
                onClick = { onPendingChange(pending - 1) }
            )

            Text(
                text = if (pending == 0) {
                    "Rate ${Money.format(ratePaise)} each"
                } else {
                    "$pending × ${Money.format(ratePaise)} = ${Money.format(pending.toLong() * ratePaise)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onAdd,
                enabled = pending > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("  Add to today")
            }
        }
    }

    if (typing) {
        NumberDialog(
            title = "$title count",
            initial = pending,
            onDismiss = { typing = false },
            onConfirm = {
                typing = false
                onPendingChange(it)
            }
        )
    }
}

/** A compact stepper plus Add, for the calendar day editor where space is tighter. */
@Composable
fun CompactComposer(
    label: String,
    pending: Int,
    onPendingChange: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalIconButton(onClick = { onPendingChange(pending - 1) }, enabled = pending > 0) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Remove one from $label")
        }
        RollingNumber(
            value = pending,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.widthIn(min = 40.dp)
        )
        FilledTonalIconButton(onClick = { onPendingChange(pending + 1) }) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Add one to $label")
        }
        Button(
            onClick = onAdd,
            enabled = pending > 0,
            modifier = Modifier.weight(1f)
        ) {
            Text("Add $label")
        }
    }
}

/** One logged order: how many, at what time, with a delete for mistaken taps. */
@Composable
fun OrderEntryRow(
    entry: OrderEntry,
    ratePaise: Long,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${entry.count} ${entry.item.label.lowercase()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${entry.recordedAt.format(DateFormats.timeOfDay)} · " +
                    Money.format(entry.count.toLong() * ratePaise),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Close, contentDescription = "Delete this order")
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 110.dp, height = 60.dp),
        shape = MaterialTheme.shapes.medium,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        // The default 24dp icon looks lost in a button this size.
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun NumberDialog(
    title: String,
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(4) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = value == null,
                supportingText = { if (value == null) Text("Enter a number") }
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PickDateDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    // The Material date picker works in UTC millis, so convert on the way in and out.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * 86_400_000L
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

/** A label/value row, used for the summary blocks on the calendar and due screens. */
@Composable
fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasise: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasise) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = if (emphasise) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal
        )
    }
}
