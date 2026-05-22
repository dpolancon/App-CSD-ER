package com.example.data.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun generateSimulatedReceipt(bankName: String, amount: Double, refId: String): Bitmap {
        val width = 600
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()

        // 1. Background
        paint.color = 0xFFF5F5F7.toInt() // light gray
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 2. Receipt Card body
        paint.color = 0xFFFFFFFF.toInt() // white card
        val cardLeft = 40f
        val cardTop = 60f
        val cardRight = width.toFloat() - 40f
        val cardBottom = height.toFloat() - 60f
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 24f, 24f, paint)

        // 3. Header Logo Circle
        paint.color = if (bankName.contains("Mercado", true)) 0xFF00B1EA.toInt() else 0xFFD32F2F.toInt() // Custom colors
        val circleX = width / 2.0f
        val circleY = 140f
        canvas.drawCircle(circleX, circleY, 40f, paint)

        // 4. Draw Logo Icon (✓ icon)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 36f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("✓", circleX, circleY + 12f, paint)

        // 5. Bank Header
        paint.color = 0xFF121212.toInt()
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText(bankName.uppercase(), circleX, 220f, paint)

        paint.color = 0xFF757575.toInt()
        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText("Comprobante de Transferencia", circleX, 255f, paint)

        // Centered divider line
        paint.color = 0xFFE0E0E0.toInt()
        canvas.drawLine(80f, 280f, width.toFloat() - 80f, 280f, paint)

        // 6. Transferred Amount
        paint.color = 0xFF121212.toInt()
        paint.textSize = 46f
        paint.isFakeBoldText = true
        canvas.drawText("$${String.format("%,.2f", amount)}", circleX, 350f, paint)

        paint.color = 0xFF4CAF50.toInt() // Green success text
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("¡TRANSFERENCIA EXITOSA!", circleX, 390f, paint)

        // Details divider line
        paint.color = 0xFFE0E0E0.toInt()
        canvas.drawLine(80f, 420f, width.toFloat() - 80f, 420f, paint)

        // 7. Info Grid
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.color = 0xFF757575.toInt()
        paint.textSize = 16f
        paint.isFakeBoldText = false

        val labelsY = listOf(470f, 520f, 570f, 620f, 670f)
        val valueLabels = listOf(
            "Referencia" to refId,
            "Destino" to "Socio Club Fútbol",
            "CBU Destino" to "0170420010000030405060",
            "Fecha" to "22/05/2026 19:15:35",
            "Motivo" to "Pago de Cuota Activa"
        )

        valueLabels.forEachIndexed { index, pair ->
            val y = labelsY[index]
            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.color = 0xFF757575.toInt()
            canvas.drawText(pair.first, 80f, y, paint)

            paint.textAlign = android.graphics.Paint.Align.RIGHT
            paint.color = 0xFF121212.toInt()
            paint.isFakeBoldText = true
            canvas.drawText(pair.second, width.toFloat() - 80f, y, paint)
        }

        // Draw physical looking receipt borders
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.color = 0xFF9E9E9E.toInt()
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Documento digital emitido por la entidad bancaria liquidadora autorizada.", circleX, 740f, paint)

        return bitmap
    }

    data class ReceiptAnalysis(
        val success: Boolean,
        val company: String = "",
        val transactionId: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val isReceiptValid: Boolean = false,
        val confidenceScore: Double = 0.0,
        val extraNotes: String = ""
    )

    // Helper to convert Bitmap to Base64
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress the bitmap to fit inside request body elegantly (max 1024 width/height is plenty for receipts)
        val scaledBitmap = scaleDownIfNeeded(this, 1024)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleDownIfNeeded(realImage: Bitmap, maxResolution: Int): Bitmap {
        val ratio = Math.min(
            maxResolution.toFloat() / realImage.width,
            maxResolution.toFloat() / realImage.height
        )
        if (ratio >= 1.0f) return realImage

        val newWidth = Math.round(ratio * realImage.width)
        val newHeight = Math.round(ratio * realImage.height)

        return Bitmap.createScaledBitmap(realImage, newWidth, newHeight, true)
    }

    suspend fun analyzeReceipt(bitmap: Bitmap): ReceiptAnalysis = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is using default placeholder.")
            return@withContext ReceiptAnalysis(
                success = false,
                extraNotes = "Error: La clave de la API de Gemini no está configurada. Por favor, ingrésala en el panel de secretos en AI Studio."
            )
        }

        try {
            val base64Image = bitmap.toBase64()

            // Construct prompt asking for specific JSON format
            val prompt = """
                Analiza este comprobante de pago de transferencia bancaria o pago de servicios.
                Extrae detalladamente los siguientes datos en un formato JSON estructurado rígido. No devuelvas markdown adicionales, solo el texto JSON de respuesta.
                Especificación JSON requerida:
                {
                  "company": "Nombre del banco o billetera digital originaria (ej: Mercado Pago, Galicia, Santander, Brubank, Ualá, etc.)",
                  "transactionId": "Número de operación, referencia, hash de transferencia o código de transacción único encontrado en la imagen. De lo contrario, dejar vacío.",
                  "amount": "Monto total de dinero pagado o transferido como número decimal (ej: 5000.0). De lo contrario, 0.0.",
                  "date": "Fecha y hora de la transacción encontradas en el comprobante, formateadas como texto legible de forma general.",
                  "isReceiptValid": true/false (si el archivo es verdaderamente un comprobante de transferencia bancaria de dinero exitoso),
                  "confidenceScore": 0.0 a 1.0 (probabilidad estimada de análisis),
                  "extraNotes": "Cualquier nota adicional, advertencia de lectura o descripción corta del motivo."
                }
            """.trimIndent()

            // Build request using org.json
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                }
                put("contents", contentsArray)
                
                // Set system instruction for robust output format
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Eres un analizador de comprobantes experto. Devuelve ÚNICAMENTE el código JSON especificado siguiendo la estructura de llaves exacta, sin formato de código estilo ```json, de modo que pueda parsearse de forma directa.")
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequest.toString().toRequestBody(mediaType)
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = "HTTP Error ${response.code}: ${response.message}"
                    Log.e(TAG, errMsg)
                    return@withContext ReceiptAnalysis(success = false, extraNotes = "Error en el servidor de Gemini: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Raw Gemini response: $responseBody")

                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext ReceiptAnalysis(success = false, extraNotes = "No se encontraron candidatos de respuesta.")
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                if (content == null) {
                    return@withContext ReceiptAnalysis(success = false, extraNotes = "Contenido de candidatos inválido.")
                }

                val parts = content.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    return@withContext ReceiptAnalysis(success = false, extraNotes = "Partes de respuesta vacías.")
                }

                var textResponse = parts.getJSONObject(0).optString("text") ?: ""
                
                // Clean markdown format if returned
                if (textResponse.startsWith("```json")) {
                    textResponse = textResponse.removePrefix("```json")
                }
                if (textResponse.endsWith("```")) {
                    textResponse = textResponse.removeSuffix("```")
                }
                textResponse = textResponse.trim()

                Log.d(TAG, "Cleaned JSON Text: $textResponse")

                val parsedAnalysis = JSONObject(textResponse)
                return@withContext ReceiptAnalysis(
                    success = true,
                    company = parsedAnalysis.optString("company", "Desconocido"),
                    transactionId = parsedAnalysis.optString("transactionId", "No encontrado"),
                    amount = parsedAnalysis.optDouble("amount", 0.0),
                    date = parsedAnalysis.optString("date", "No encontrado"),
                    isReceiptValid = parsedAnalysis.optBoolean("isReceiptValid", false),
                    confidenceScore = parsedAnalysis.optDouble("confidenceScore", 0.0),
                    extraNotes = parsedAnalysis.optString("extraNotes", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during analyzeReceipt: ", e)
            return@withContext ReceiptAnalysis(
                success = false,
                extraNotes = "Excepción al procesar imagen: ${e.localizedMessage}"
            )
        }
    }
}
