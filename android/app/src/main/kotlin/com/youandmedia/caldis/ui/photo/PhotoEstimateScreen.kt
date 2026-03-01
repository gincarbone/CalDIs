package com.youandmedia.caldis.ui.photo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.youandmedia.caldis.ai.GeminiService
import com.youandmedia.caldis.util.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEstimateScreen(
    date: LocalDate,
    viewModel: PhotoEstimateViewModel,
    autoLaunchCamera: Boolean = false,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val photoBitmap = state.photoBitmap
    var selectedCategory by remember { mutableStateOf(constTipoPasti.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("caldis_prefs", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var availableModels by remember { mutableStateOf(emptyList<String>()) }
    var modelsLoading by remember { mutableStateOf(false) }
    var modelsError by remember { mutableStateOf<String?>(null) }
    var showModelDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val weightRanges = listOf("50-100", "100-150", "150-200", "200-300", "300-400")
    var useWeightHint by remember { mutableStateOf(false) }
    var selectedWeightIndex by remember { mutableStateOf(2f) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPhoto(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.setPhoto(it) }
        } else {
            Toast.makeText(context, "Scatto annullato.", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val outputUri = createCameraOutputUri(context)
            if (outputUri == null) {
                Toast.makeText(context, "Impossibile creare il file foto.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            pendingCameraUri = outputUri
            runCatching { cameraLauncher.launch(outputUri) }
                .onFailure {
                    Toast.makeText(context, "Impossibile aprire la fotocamera su questo dispositivo.", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(context, "Permesso fotocamera negato.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) {
            Toast.makeText(context, "Nessuna fotocamera disponibile su questo dispositivo.", Toast.LENGTH_LONG).show()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val outputUri = createCameraOutputUri(context)
            if (outputUri == null) {
                Toast.makeText(context, "Impossibile creare il file foto.", Toast.LENGTH_LONG).show()
                return
            }
            pendingCameraUri = outputUri
            runCatching { cameraLauncher.launch(outputUri) }
                .onFailure {
                    Toast.makeText(context, "Impossibile aprire la fotocamera su questo dispositivo.", Toast.LENGTH_LONG).show()
                }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun loadModelsAndOpenPopup() {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            Toast.makeText(context, "API key mancante. Inseriscila in Impostazioni.", Toast.LENGTH_LONG).show()
            return
        }

        modelsLoading = true
        modelsError = null
        scope.launch {
            GeminiService.listAvailableGenerateModels(apiKey, onlyFlashModels = true)
                .onSuccess { models ->
                    availableModels = models
                    if (models.isEmpty()) {
                        modelsError = "Nessun modello Flash disponibile per questa API key."
                    } else {
                        showModelDialog = true
                    }
                }
                .onFailure { err ->
                    modelsError = err.message ?: "Errore nel caricamento modelli."
                }
            modelsLoading = false
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    LaunchedEffect(state.photoTakenAtMillis) {
        val suggested = state.autoSuggestedCategory
        if (!suggested.isNullOrBlank() && constTipoPasti.contains(suggested)) {
            selectedCategory = suggested
        }
    }
    LaunchedEffect(autoLaunchCamera) {
        if (autoLaunchCamera && state.photoBitmap == null && state.photoUri == null) {
            openCamera()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stima Calorie da Foto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        containerColor = BgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo preview or placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                if (state.photoUri != null) {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Foto piatto",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Foto piatto",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Seleziona una foto del piatto", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientTeal)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galleria", fontSize = 12.sp)
                }
                Button(
                    onClick = { openCamera() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientGreen)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fotocamera", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Suggerimento: inserisci nella foto una carta (tipo carta di credito) per migliorare la stima delle porzioni.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            if (state.photoBitmap != null && state.estimate == null && !state.isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Peso stimato (opzionale)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = useWeightHint,
                                onCheckedChange = { useWeightHint = it }
                            )
                        }
                        if (useWeightHint) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Seleziona la fascia peso totale del piatto",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Slider(
                                value = selectedWeightIndex,
                                onValueChange = { selectedWeightIndex = it },
                                valueRange = 0f..(weightRanges.lastIndex.toFloat()),
                                steps = weightRanges.size - 2
                            )
                            Text(
                                "${weightRanges[selectedWeightIndex.toInt()]} g",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GradientGreen
                            )
                        }
                    }
                }
            }

            if (state.photoBitmap != null && state.estimate == null && !state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val weightHint = if (useWeightHint) {
                            weightRanges[selectedWeightIndex.toInt()]
                        } else {
                            null
                        }
                        viewModel.analyzePhoto(weightHint)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GradientGreen)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analizza con Gemini AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Loading
            if (state.isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = GradientGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analizzando il piatto...", color = Color.Gray, fontSize = 14.sp)
            }

            // Results
            state.estimate?.let { estimate ->
                Spacer(modifier = Modifier.height(16.dp))

                if (estimate.error != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                estimate.error,
                                color = Color(0xFFC62828),
                                fontSize = 14.sp
                            )
                            val isModelError = estimate.error.contains("modello", ignoreCase = true) ||
                                estimate.error.contains("not found", ignoreCase = true) ||
                                estimate.error.contains("no longer available", ignoreCase = true)
                            if (isModelError) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { loadModelsAndOpenPopup() },
                                    enabled = !modelsLoading,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (modelsLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(if (modelsLoading) "Caricamento..." else "Scegli modello (popup)")
                                }
                                if (modelsError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        modelsError ?: "",
                                        color = Color(0xFFC62828),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (estimate.latencyMs != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tempo risposta AI: ${estimate.latencyMs} ms",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    var manualCaloriesText by remember(estimate.totalMin, estimate.totalMax) {
                        mutableStateOf(((estimate.totalMin + estimate.totalMax) / 2).toString())
                    }
                    // Results header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Risultati Analisi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                            Spacer(modifier = Modifier.height(12.dp))

                            estimate.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(item.portion, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text(
                                        "${item.caloriesMin}-${item.caloriesMax} kcal",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MealColor
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Total
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GradientGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Totale stimato", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${estimate.totalMin}-${estimate.totalMax} kcal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GradientGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = manualCaloriesText,
                                onValueChange = { manualCaloriesText = it.filter { c -> c.isDigit() } },
                                label = { Text("Calorie finali da salvare") },
                                singleLine = true,
                                trailingIcon = { Text("kcal", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            if (estimate.latencyMs != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Tempo risposta AI: ${estimate.latencyMs} ms",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            if (!estimate.debugInfo.isNullOrBlank()) {
                                Text(
                                    estimate.debugInfo,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category selector for saving
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoria pasto") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            constTipoPasti.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save button
                    Button(
                        onClick = {
                            val manualCalories = manualCaloriesText.toDoubleOrNull()
                            viewModel.saveAsMeal(date, selectedCategory, manualCalories)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GradientGreen)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Conferma e Salva (${manualCaloriesText.ifBlank { ((estimate.totalMin + estimate.totalMax) / 2).toString() }} kcal)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
                                    val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                                    prefs.edit().putString("gemini_model", model).apply()
                                    if (apiKey.isNotBlank()) {
                                        GeminiService.initialize(apiKey, model)
                                    }
                                    Toast.makeText(context, "Modello salvato: $model", Toast.LENGTH_SHORT).show()
                                    showModelDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(model) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModelDialog = false }) { Text("Chiudi") }
                }
            )
        }
    }
}

private fun createCameraOutputUri(context: Context): Uri? {
    return runCatching {
        val photosDir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File.createTempFile("caldis_photo_", ".jpg", photosDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}
