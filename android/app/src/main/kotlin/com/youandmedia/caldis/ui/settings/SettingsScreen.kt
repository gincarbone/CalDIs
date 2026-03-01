@file:OptIn(ExperimentalMaterial3Api::class)

package com.youandmedia.caldis.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.youandmedia.caldis.ai.GeminiService
import com.youandmedia.caldis.util.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("caldis_prefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var selectedModel by remember { mutableStateOf(prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash") }
    var availableModels by remember { mutableStateOf(emptyList<String>()) }
    var modelsLoading by remember { mutableStateOf(false) }
    var modelsError by remember { mutableStateOf<String?>(null) }
    var showModelDialog by remember { mutableStateOf(false) }
    var dailyBudget by remember { mutableStateOf(prefs.getFloat("daily_calorie_budget", DEFAULT_DAILY_CALORIE_BUDGET.toFloat()).toString()) }
    var showApiKey by remember { mutableStateOf(false) }
    var showBmrDialog by remember { mutableStateOf(false) }
    var sex by remember { mutableStateOf(prefs.getString("profile_sex", "Uomo") ?: "Uomo") }
    var ageValue by remember { mutableIntStateOf(prefs.getInt("profile_age", 30).coerceIn(18, 90)) }
    var heightValue by remember { mutableIntStateOf(prefs.getFloat("profile_height_cm", 170f).toInt().coerceIn(150, 190)) }
    var weightValue by remember { mutableIntStateOf(prefs.getFloat("profile_weight_kg", 70f).toInt().coerceIn(30, 300)) }
    var activityLabel by remember { mutableStateOf(prefs.getString("profile_activity", "Moderatamente attivo (1.55)") ?: "Moderatamente attivo (1.55)") }
    var goalLabel by remember { mutableStateOf(prefs.getString("profile_goal", "Mantenimento (0 kcal)") ?: "Mantenimento (0 kcal)") }

    fun loadModels() {
        if (apiKey.isBlank()) {
            modelsError = "Inserisci prima la API key."
            return
        }

        modelsLoading = true
        modelsError = null
        scope.launch {
            val result = GeminiService.listAvailableGenerateModels(apiKey.trim(), onlyFlashModels = true)
            result
                .onSuccess { models ->
                    availableModels = models
                    if (models.isEmpty()) {
                        modelsError = "Nessun modello disponibile trovato per questa API key."
                    } else if (selectedModel !in models) {
                        selectedModel = models.first()
                    }
                }
                .onFailure { error ->
                    modelsError = error.message ?: "Errore nel caricamento dei modelli."
                }
            modelsLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Impostazioni", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        Spacer(modifier = Modifier.height(16.dp))

        // App info card
        Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(GradientGreen, GradientTeal)))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("CalDis", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Versione $APP_VERSION", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Il tuo diario alimentare intelligente con stima calorie da foto AI.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Configuration
        Text("Configurazione AI", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        prefs.edit().putString("gemini_api_key", apiKey).apply()
                        prefs.edit().putString("gemini_model", selectedModel).apply()
                        GeminiService.initialize(apiKey, selectedModel)
                        Toast.makeText(context, "API Key e modello salvati!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientGreen)
                ) {
                    Text("Salva API Key")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Ottieni una API key gratuita su aistudio.google.com",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { loadModels() },
                    enabled = !modelsLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    if (modelsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (modelsLoading) "Caricamento modelli..." else "Carica modelli disponibili")
                }
                if (modelsError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = modelsError ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFC62828)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modello Gemini selezionato") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (availableModels.isEmpty()) {
                            Toast.makeText(context, "Prima carica i modelli disponibili.", Toast.LENGTH_SHORT).show()
                        } else {
                            showModelDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scegli modello (popup)")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Mostriamo solo modelli Flash disponibili per la tua API key.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        if (showModelDialog) {
            AlertDialog(
                onDismissRequest = { showModelDialog = false },
                title = { Text("Seleziona modello Flash") },
                text = {
                    Column {
                        availableModels.forEach { model ->
                            TextButton(
                                onClick = {
                                    selectedModel = model
                                    prefs.edit().putString("gemini_model", model).apply()
                                    if (apiKey.isNotBlank()) {
                                        GeminiService.initialize(apiKey, model)
                                    }
                                    showModelDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(model)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModelDialog = false }) { Text("Chiudi") }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calorie Budget
        Text("Budget Calorico", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = dailyBudget,
                    onValueChange = { dailyBudget = it.replace(',', '.') },
                    label = { Text("Budget giornaliero (kcal)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val budget = dailyBudget.toFloatOrNull()
                        if (budget != null && budget > 0) {
                            prefs.edit().putFloat("daily_calorie_budget", budget).apply()
                            Toast.makeText(context, "Budget salvato: ${budget.toInt()} kcal/giorno", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Text("Salva Budget")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showBmrDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calcola con motore BMR/TDEE")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Calcola automaticamente il budget calorico giornaliero da profilo, attivita' e obiettivo.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        if (showBmrDialog) {
            val activityOptions = listOf(
                "Sedentario (1.20)" to 1.20,
                "Leggermente attivo (1.375)" to 1.375,
                "Moderatamente attivo (1.55)" to 1.55,
                "Molto attivo (1.725)" to 1.725,
                "Estremamente attivo (1.90)" to 1.90
            )
            val goalOptions = listOf(
                "Deficit leggero (-300 kcal)" to -300.0,
                "Deficit medio (-500 kcal)" to -500.0,
                "Mantenimento (0 kcal)" to 0.0,
                "Surplus leggero (+250 kcal)" to 250.0,
                "Surplus medio (+400 kcal)" to 400.0
            )
            var activityExpanded by remember { mutableStateOf(false) }
            var goalExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showBmrDialog = false },
                title = { Text("Motore BMR/TDEE") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sesso biologico", fontSize = 12.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GenderChoiceCard(
                                label = "Uomo",
                                icon = Icons.Default.Male,
                                selected = sex == "Uomo",
                                onClick = { sex = "Uomo" },
                                modifier = Modifier.weight(1f)
                            )
                            GenderChoiceCard(
                                label = "Donna",
                                icon = Icons.Default.Female,
                                selected = sex == "Donna",
                                onClick = { sex = "Donna" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        NumberAdjustField(
                            label = "Eta' (anni)",
                            value = ageValue,
                            range = 18..90,
                            onChange = { ageValue = it }
                        )
                        NumberAdjustField(
                            label = "Altezza (cm)",
                            value = heightValue,
                            range = 150..190,
                            onChange = { heightValue = it }
                        )
                        NumberAdjustField(
                            label = "Peso (kg)",
                            value = weightValue,
                            range = 30..300,
                            onChange = { weightValue = it }
                        )

                        ExposedDropdownMenuBox(
                            expanded = activityExpanded,
                            onExpandedChange = { activityExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = activityLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Livello attivita'") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = activityExpanded,
                                onDismissRequest = { activityExpanded = false }
                            ) {
                                activityOptions.forEach { (label, _) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            activityLabel = label
                                            activityExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = goalExpanded,
                            onExpandedChange = { goalExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = goalLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Obiettivo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = goalExpanded,
                                onDismissRequest = { goalExpanded = false }
                            ) {
                                goalOptions.forEach { (label, _) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            goalLabel = label
                                            goalExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val age = ageValue
                            val height = heightValue.toDouble()
                            val weight = weightValue.toDouble()
                            val activityFactor = activityOptions.firstOrNull { it.first == activityLabel }?.second
                            val goalAdjust = goalOptions.firstOrNull { it.first == goalLabel }?.second

                            if (activityFactor == null || goalAdjust == null ||
                                age !in 18..90 || height !in 150.0..190.0 || weight !in 30.0..300.0
                            ) {
                                Toast.makeText(context, "Controlla i dati inseriti (eta', altezza, peso).", Toast.LENGTH_LONG).show()
                                return@TextButton
                            }

                            val bmr = if (sex == "Uomo") {
                                10.0 * weight + 6.25 * height - 5.0 * age + 5.0
                            } else {
                                10.0 * weight + 6.25 * height - 5.0 * age - 161.0
                            }
                            val tdee = bmr * activityFactor
                            val target = (tdee + goalAdjust).coerceAtLeast(1200.0)

                            dailyBudget = String.format(java.util.Locale.US, "%.0f", target)
                            prefs.edit()
                                .putFloat("daily_calorie_budget", target.toFloat())
                                .putString("profile_sex", sex)
                                .putInt("profile_age", age)
                                .putFloat("profile_height_cm", height.toFloat())
                                .putFloat("profile_weight_kg", weightValue.toFloat())
                                .putString("profile_activity", activityLabel)
                                .putString("profile_goal", goalLabel)
                                .apply()

                            Toast.makeText(
                                context,
                                "BMR: ${bmr.toInt()} kcal | TDEE: ${tdee.toInt()} kcal | Budget: ${target.toInt()} kcal",
                                Toast.LENGTH_LONG
                            ).show()
                            showBmrDialog = false
                        }
                    ) { Text("Calcola e Applica") }
                },
                dismissButton = {
                    TextButton(onClick = { showBmrDialog = false }) { Text("Annulla") }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions section
        Text("Azioni", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "Condividi App",
                    subtitle = "Condividi l'APK con i tuoi amici",
                    iconColor = Color(0xFF45B7D1),
                    onClick = { shareApk(context) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Esporta Report PDF",
                    subtitle = "Esporta il report alimentare mensile in PDF",
                    iconColor = Color(0xFFE74C3C),
                    onClick = { exportMonthlyPdf(context) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Pulisci Cache",
                    subtitle = "Libera spazio eliminando file temporanei",
                    iconColor = Color(0xFFF39C12),
                    onClick = {
                        context.cacheDir.deleteRecursively()
                        Toast.makeText(context, "Cache pulita!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info section
        Text("Informazioni", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Column {
                InfoRow("Versione App", APP_VERSION)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow("Sviluppatore", "You&Media")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow("Piattaforma", "Android ${android.os.Build.VERSION.RELEASE}")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow("AI Engine", "Google Gemini")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow("Architettura", "Kotlin + Jetpack Compose")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Made with", color = Color.Gray, fontSize = 12.sp)
                Text("You&Media", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2C3E50))
                Text("youandmedia.it", color = Color(0xFF45B7D1), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NumberAdjustField(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onChange((value - 1).coerceIn(range.first, range.last)) }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Diminuisci")
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it.filter { ch -> ch.isDigit() }.take(3)
                    val parsed = textValue.toIntOrNull()
                    if (parsed != null) onChange(parsed.coerceIn(range.first, range.last))
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF2C3E50)
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            IconButton(
                onClick = { onChange((value + 1).coerceIn(range.first, range.last)) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aumenta")
            }
        }
    }
}

@Composable
private fun GenderChoiceCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) GradientGreen else Color(0xFFD0D7DE)
    val background = if (selected) GradientGreen.copy(alpha = 0.12f) else Color(0xFFF8F9FA)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) GradientGreen else Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, iconColor: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF7F8C8D))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
    }
}

private fun shareApk(context: Context) {
    try {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        val sourceApk = File(appInfo.sourceDir)
        val cacheDir = File(context.cacheDir, "apk")
        cacheDir.mkdirs()
        val destApk = File(cacheDir, "CalDis.apk")
        sourceApk.copyTo(destApk, overwrite = true)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destApk)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Condividi CalDis"))
    } catch (e: Exception) {
        Toast.makeText(context, "Errore nella condivisione: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun exportMonthlyPdf(context: Context) {
    try {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }

        canvas.drawText("CalDis - Report Alimentare Mensile", 50f, 60f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false

        val now = java.time.YearMonth.now()
        val monthName = now.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
        canvas.drawText("Mese: $monthName ${now.year}", 50f, 100f, paint)
        canvas.drawText("Data generazione: ${java.time.LocalDate.now()}", 50f, 120f, paint)

        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Riepilogo", 50f, 170f, paint)

        paint.textSize = 13f
        paint.isFakeBoldText = false

        val db = (context.applicationContext as com.youandmedia.caldis.CaldisApp).database
        val zone = java.time.ZoneId.systemDefault()
        val startOfMonth = now.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfMonth = now.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        kotlinx.coroutines.runBlocking {
            val meals = db.mealDao().getByDateRangeOnce(startOfMonth, endOfMonth)
            val activities = db.activityDao().getByDateRangeOnce(startOfMonth, endOfMonth)
            val fixedMeals = db.fixedMealDao().getAllOnce()
            val fixedActivities = db.fixedActivityDao().getAllOnce()

            val totalCal = meals.sumOf { it.calories }
            val totalBurned = activities.sumOf { it.caloriesBurned }
            val totalFixedMeal = fixedMeals.sumOf { it.calories }
            val totalFixedAct = fixedActivities.sumOf { it.caloriesBurned }

            var y = 200f
            canvas.drawText("Calorie consumate: ${String.format("%.0f", totalCal)} kcal", 50f, y, paint); y += 22f
            canvas.drawText("Calorie bruciate: ${String.format("%.0f", totalBurned)} kcal", 50f, y, paint); y += 22f
            canvas.drawText("Pasti ricorrenti: ${String.format("%.0f", totalFixedMeal)} kcal", 50f, y, paint); y += 22f
            canvas.drawText("Attivit\u00E0 ricorrenti: ${String.format("%.0f", totalFixedAct)} kcal", 50f, y, paint); y += 22f

            paint.isFakeBoldText = true
            paint.textSize = 15f
            y += 10f
            canvas.drawText("Bilancio netto: ${String.format("%.0f", totalCal - totalBurned)} kcal", 50f, y, paint)

            y += 40f
            paint.textSize = 16f
            canvas.drawText("Dettaglio per Categoria", 50f, y, paint)

            paint.textSize = 13f
            paint.isFakeBoldText = false
            y += 25f

            val byCat = meals.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.calories } }
                .toList()
                .sortedByDescending { it.second }

            for ((cat, cal) in byCat) {
                val pct = if (totalCal > 0) cal / totalCal * 100 else 0.0
                canvas.drawText("$cat: ${String.format("%.0f", cal)} kcal (${String.format("%.1f", pct)}%)", 50f, y, paint)
                y += 20f
                if (y > 780f) break
            }

            y += 15f
            paint.isFakeBoldText = true
            paint.textSize = 16f
            if (y < 700f) {
                canvas.drawText("Lista Pasti", 50f, y, paint)
                paint.textSize = 11f
                paint.isFakeBoldText = false
                y += 20f

                for (meal in meals.sortedByDescending { it.date }) {
                    val dateStr = java.time.Instant.ofEpochMilli(meal.date).atZone(zone).toLocalDate().toString()
                    val line = "$dateStr  ${meal.category}  ${String.format("%.0f", meal.calories)} kcal  ${meal.description}"
                    canvas.drawText(line, 50f, y, paint)
                    y += 16f
                    if (y > 800f) break
                }
            }
        }

        pdfDoc.finishPage(page)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "CalDis_Report_${java.time.YearMonth.now()}.pdf")
        pdfDoc.writeTo(file.outputStream())
        pdfDoc.close()

        Toast.makeText(context, "PDF salvato in Downloads: ${file.name}", Toast.LENGTH_LONG).show()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(openIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Errore generazione PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
