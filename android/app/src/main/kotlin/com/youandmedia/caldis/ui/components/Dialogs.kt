package com.youandmedia.caldis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandmedia.caldis.util.MealColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDialog(
    title: String,
    categories: List<String>,
    initialCategory: String? = null,
    initialAmount: String = "",
    initialNotes: String = "",
    amountColor: Color = MealColor,
    amountLabel: String = "Calorie (kcal)",
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double, notes: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var notesText by remember { mutableStateOf(initialNotes) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleziona Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCategory != null) {
                    Text(
                        "Categoria: $selectedCategory",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 14.sp
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.replace(',', '.') },
                    label = { Text(amountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { if (it.length <= 24) notesText = it },
                    label = { Text("Note") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${notesText.length}/24") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (selectedCategory != null && amount != null) {
                        onConfirm(selectedCategory!!, amount, notesText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Conferma", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {}
    )
}

@Composable
fun ConfirmDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Conferma Cancellazione",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sei sicuro di voler cancellare questo elemento?")
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Red)
                            .clip(RoundedCornerShape(bottomStart = 20.dp)),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text("ANNULLA", color = Color.White)
                    }
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF2E7D32))
                            .clip(RoundedCornerShape(bottomEnd = 20.dp)),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text("CONFERMA", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFixedEntryDialog(
    isMeal: Boolean,
    categories: List<String>,
    initialCategory: String? = null,
    initialAmount: String = "",
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isMeal) "Aggiungi Pasto Fisso" else "Aggiungi Attivit\u00E0 Fissa")
        },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleziona Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.replace(',', '.') },
                    label = { Text(if (isMeal) "Calorie (kcal)" else "Calorie bruciate (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (selectedCategory != null && amount != null) {
                    onSave(selectedCategory!!, amount)
                }
            }) {
                Text("Salva")
            }
        },
        dismissButton = {}
    )
}
