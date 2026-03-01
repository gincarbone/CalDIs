package com.youandmedia.caldis.ui.photo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandmedia.caldis.CaldisApp
import com.youandmedia.caldis.ai.CalorieEstimate
import com.youandmedia.caldis.ai.GeminiService
import com.youandmedia.caldis.data.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PhotoEstimateUiState(
    val photoUri: Uri? = null,
    val photoBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val estimate: CalorieEstimate? = null,
    val saved: Boolean = false,
    val autoSuggestedCategory: String? = null,
    val photoTakenAtMillis: Long? = null
)

class PhotoEstimateViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CaldisApp).database

    private val _uiState = MutableStateFlow(PhotoEstimateUiState())
    val uiState: StateFlow<PhotoEstimateUiState> = _uiState

    fun setPhoto(uri: Uri) {
        val context = getApplication<CaldisApp>()
        val bitmap = try {
            decodeNormalizedBitmap(context, uri)
        } catch (e: Exception) {
            null
        }
        val takenAtMillis = extractPhotoTakenAtMillis(context, uri)
        val localTime = java.time.Instant.ofEpochMilli(takenAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val suggestedCategory = suggestMealCategory(localTime)

        _uiState.value = _uiState.value.copy(
            photoUri = uri,
            photoBitmap = bitmap,
            estimate = null,
            autoSuggestedCategory = suggestedCategory,
            photoTakenAtMillis = takenAtMillis
        )
    }

    fun setPhoto(bitmap: Bitmap) {
        val normalized = resizeBitmap(bitmap)
        val now = System.currentTimeMillis()
        val suggestedCategory = suggestMealCategory(
            java.time.Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
        )
        _uiState.value = _uiState.value.copy(
            photoUri = null,
            photoBitmap = normalized,
            estimate = null,
            autoSuggestedCategory = suggestedCategory,
            photoTakenAtMillis = now
        )
    }

    fun analyzePhoto(weightRangeHint: String? = null) {
        val bitmap = _uiState.value.photoBitmap ?: return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val estimate = GeminiService.estimateCalories(bitmap, weightRangeHint)
            _uiState.value = _uiState.value.copy(isLoading = false, estimate = estimate)
        }
    }

    fun saveAsMeal(date: LocalDate, category: String, caloriesOverride: Double? = null) {
        val estimate = _uiState.value.estimate ?: return
        val avgCalories = (estimate.totalMin + estimate.totalMax) / 2.0
        val finalCalories = caloriesOverride ?: avgCalories
        val description = estimate.items.joinToString(", ") { it.name }

        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.mealDao().insert(
                Meal(
                    calories = finalCalories,
                    category = category,
                    description = description.take(24),
                    date = millis,
                    photoUri = _uiState.value.photoUri?.toString()
                )
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    private fun decodeNormalizedBitmap(context: CaldisApp, uri: Uri): Bitmap? {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        return resizeBitmap(applyOrientation(decoded, orientation))
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 768): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun extractPhotoTakenAtMillis(context: CaldisApp, uri: Uri): Long {
        return runCatching {
            val exifDateString = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            }
            if (exifDateString.isNullOrBlank()) return@runCatching System.currentTimeMillis()

            val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            val dateTime = LocalDateTime.parse(exifDateString, formatter)
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }
    }

    private fun suggestMealCategory(time: LocalTime): String {
        val hour = time.hour
        return when (hour) {
            in 5..10 -> "Colazione"
            in 11..14 -> "Pranzo"
            in 18..22 -> "Cena"
            else -> "Snack"
        }
    }
}
