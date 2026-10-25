package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Configure shift attendance payment rates and penalties per attendance outcome.
 */
@Composable
fun ShiftSalaryRuleDialog(
    staffName: String,
    initialPresentPay: String = "600",
    initialLatePenalty: String = "50",
    initialHalfDayPay: String = "300",
    initialHolidayPay: String = "600",
    initialOffDayPay: String = "600",
    onDismiss: () -> Unit,
    onConfirm: (
        presentPay: String,
        latePenalty: String,
        halfDayPay: String,
        holidayPay: String,
        offDayPay: String
    ) -> Unit
) {
    var presentPay by remember { mutableStateOf(initialPresentPay) }
    var latePenalty by remember { mutableStateOf(initialLatePenalty) }
    var halfDayPay by remember { mutableStateOf(initialHalfDayPay) }
    var holidayPay by remember { mutableStateOf(initialHolidayPay) }
    var offDayPay by remember { mutableStateOf(initialOffDayPay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Shift Salary Rates", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Staff: $staffName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = presentPay,
                    onValueChange = { presentPay = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Present Shift Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = latePenalty,
                    onValueChange = { latePenalty = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Late Arrival Penalty (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = halfDayPay,
                    onValueChange = { halfDayPay = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Half Day Shift Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = holidayPay,
                    onValueChange = { holidayPay = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Paid Public Holiday Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = offDayPay,
                    onValueChange = { offDayPay = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Paid Weekly Off-Day Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(presentPay, latePenalty, halfDayPay, holidayPay, offDayPay)
                },
                enabled = (Money.parseToCents(presentPay) ?: 0) > 0
            ) { Text("Save Rates") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
