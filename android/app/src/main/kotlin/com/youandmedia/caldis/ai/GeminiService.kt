package com.youandmedia.caldis.ai

import android.graphics.Bitmap
import android.os.SystemClock
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class FoodItem(
    val name: String,
    val portion: String,
    val caloriesMin: Int,
    val caloriesMax: Int
)

data class CalorieEstimate(
    val items: List<FoodItem>,
    val totalMin: Int,
    val totalMax: Int,
    val error: String? = null,
    val latencyMs: Long? = null,
    val debugInfo: String? = null
)


object GeminiService {

    private const val DEFAULT_MODEL = "gemini-2.5-flash"
    private const val MODELS_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
    private var model: GenerativeModel? = null
    private var activeModelName: String = DEFAULT_MODEL

    fun initialize(apiKey: String, modelName: String = DEFAULT_MODEL) {
        if (apiKey.isNotBlank()) {
            activeModelName = modelName.ifBlank { DEFAULT_MODEL }
            model = GenerativeModel(
                modelName = activeModelName,
                apiKey = apiKey
            )
        }
    }

    suspend fun listAvailableGenerateModels(
        apiKey: String,
        onlyFlashModels: Boolean = true
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "API Key mancante." }

            val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
            val url = URL("$MODELS_ENDPOINT?key=$encodedKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            try {
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

                if (status !in 200..299) {
                    throw IllegalStateException("Errore $status durante il recupero modelli: $body")
                }

                val json = JSONObject(body)
                val models = json.optJSONArray("models")
                    ?: return@runCatching emptyList<String>()

                val available = mutableListOf<String>()
                for (i in 0 until models.length()) {
                    val modelObj = models.getJSONObject(i)
                    val name = modelObj.optString("name")
                    val methods = modelObj.optJSONArray("supportedGenerationMethods")
                    val supportsGenerateContent = (0 until (methods?.length() ?: 0))
                        .any { idx -> methods?.optString(idx) == "generateContent" }

                    if (supportsGenerateContent && name.startsWith("models/")) {
                        available.add(name.removePrefix("models/"))
                    }
                }

                available
                    .filter { it.startsWith("gemini", ignoreCase = true) }
                    .filter { !onlyFlashModels || it.contains("flash", ignoreCase = true) }
                    .distinct()
                    .sorted()
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun estimateCalories(bitmap: Bitmap, weightRangeHint: String? = null): CalorieEstimate {
        val totalStartMs = SystemClock.elapsedRealtime()
        val currentModel = model ?: return CalorieEstimate(
            items = emptyList(), totalMin = 0, totalMax = 0,
            error = "API Key non configurata. Vai nelle Impostazioni per inserirla."
        )

        return try {
            val networkStartMs = SystemClock.elapsedRealtime()
            val response = currentModel.generateContent(
                content {
                    image(bitmap)
                    val weightHintLine = if (weightRangeHint.isNullOrBlank()) {
                        "Riferimento peso: non fornito."
                    } else {
                        "Riferimento peso totale piatto fornito dall'utente: $weightRangeHint g."
                    }
                    text("""Analizza questa foto di cibo e stima le calorie con approccio prudente.
$weightHintLine
Usa solo cibi chiaramente visibili, evita doppio conteggio, porzioni conservative.
Se disponibile usa riferimento scala (es. carta 85.6x54 mm) e/o peso totale indicato.
Se immagine incerta, mantieni stima bassa plausibile.

Rispondi SOLO con un JSON valido in questo formato esatto, senza markdown o altro testo:
{"items":[{"name":"nome alimento","portion":"porzione stimata","calories_min":numero,"calories_max":numero}],"total_min":numero,"total_max":numero}""")
                }
            )
            val networkElapsedMs = SystemClock.elapsedRealtime() - networkStartMs

            val text = response.text ?: return CalorieEstimate(
                items = emptyList(), totalMin = 0, totalMax = 0,
                error = "Nessuna risposta dal modello",
                latencyMs = SystemClock.elapsedRealtime() - totalStartMs
            )

            val parseStartMs = SystemClock.elapsedRealtime()
            val parsed = parseResponse(text)
            val parseElapsedMs = SystemClock.elapsedRealtime() - parseStartMs
            val totalElapsedMs = SystemClock.elapsedRealtime() - totalStartMs
            val debug = "modello=$activeModelName, img=${bitmap.width}x${bitmap.height}, rete+inferenza=${networkElapsedMs}ms, parsing=${parseElapsedMs}ms"
            parsed.copy(latencyMs = totalElapsedMs, debugInfo = debug)
        } catch (e: Exception) {
            val message = e.message ?: "Errore sconosciuto"
            val userMessage = if (message.contains("no longer available", ignoreCase = true)) {
                "Il modello AI configurato non e' piu' disponibile. Aggiorna l'app e riprova."
            } else if (message.contains("not found", ignoreCase = true)) {
                "Il modello AI \"$activeModelName\" non risulta disponibile per questa API key. Apri Impostazioni, carica la lista modelli e selezionane uno disponibile."
            } else {
                "Errore: $message"
            }
            CalorieEstimate(
                items = emptyList(), totalMin = 0, totalMax = 0,
                error = userMessage,
                latencyMs = SystemClock.elapsedRealtime() - totalStartMs
            )
        }
    }

    private fun parseResponse(text: String): CalorieEstimate {
        return try {
            // Extract JSON from potential markdown code blocks
            val jsonStr = text.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(jsonStr)
            val itemsArray = json.getJSONArray("items")
            val items = mutableListOf<FoodItem>()

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                items.add(FoodItem(
                    name = item.getString("name"),
                    portion = item.getString("portion"),
                    caloriesMin = item.getInt("calories_min"),
                    caloriesMax = item.getInt("calories_max")
                ))
            }

            CalorieEstimate(
                items = items,
                totalMin = json.getInt("total_min"),
                totalMax = json.getInt("total_max")
            )
        } catch (e: Exception) {
            CalorieEstimate(
                items = emptyList(), totalMin = 0, totalMax = 0,
                error = "Errore nel parsing della risposta AI: ${e.message}\n\nRisposta: $text"
            )
        }
    }
}
