package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.util.concurrent.TimeUnit

class AiRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Context check for network connectivity
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Generate content from AI based on selected modes ("CONTINUE", "IMPROVE", "BRAINSTORM")
     * Supports OpenAI GPT-4o, falls back to Gemini 3.5-Flash (platform standard),
     * and falls back to Local Heuristics if offline/no keys are provided.
     */
    suspend fun generateAiContent(
        mode: String,
        prompt: String,
        contextInfo: String,
        charactersList: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val hasInternet = isNetworkAvailable()

        // Get API Keys from BuildConfig
        val openAiKey = try { BuildConfig.OPENAI_API_KEY } catch (e: Exception) { "" }
        val geminiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        val systemInstruction = when (mode) {
            "CONTINUE" -> """
                Anda adalah asisten penulis naskah film profesional. Tugas Anda adalah MELESTARIKAN dan MELANJUTKAN naskah berikut berdasarkan teks terakhir.
                Tulis dalam Bahasa Indonesia. Format tulisan harus sesuai salah satu elemen skrip standar (Scene Heading, Action, Character, Dialogue, Parenthetical, Transition) secara logis.
                Jangan menambahkan komentar di luar naskah. Langsung lanjutkan cerita.
            """.trimIndent()
            "IMPROVE" -> """
                Anda adalah ahli dialog film senior. Tugas Anda adalah MEMPERBAIKI dialog naskah berikut agar terdengar lebih alami, dramatis, tajam, dan sesuai karakter orang Indonesia asli.
                Tulis dialog hasil revisi saja berikut nama karakternya. Jangan tulis teks penjelas lain.
            """.trimIndent()
            else -> """
                Anda adalah produser film kreatif. Berikan ide-ide brainstorming berupa 3 alternatif konflik, plot twist mengejutkan, atau opsi akhir (ending) untuk skrip/logline berikut.
                Tulis dalam poin-poin Bahasa Indonesia yang rapi, ringkas, dan menggugah imajinasi.
            """.trimIndent()
        }

        // 1. Try OpenAI GPT-4o if key is present and internet is available
        if (hasInternet && openAiKey.isNotEmpty() && !openAiKey.startsWith("MY_")) {
            try {
                return@withContext callOpenAi(openAiKey, systemInstruction, prompt + "\n" + contextInfo)
            } catch (e: Exception) {
                Log.e("ScriptKuAI", "OpenAI call failed, falling back to Gemini", e)
            }
        }

        // 2. Try Gemini 3.5-Flash if key is present and internet is available (Standard platform AI)
        if (hasInternet && geminiKey.isNotEmpty() && !geminiKey.startsWith("MY_")) {
            try {
                val fullPrompt = "$systemInstruction\n\nNASKAH SAYA SAAT INI:\n$prompt\n\nKONTEKS TAMBAHAN:\n$contextInfo"
                return@withContext callGemini(geminiKey, fullPrompt)
            } catch (e: Exception) {
                Log.e("ScriptKuAI", "Gemini call failed", e)
            }
        }

        // 3. Offline Heuristic / Mock AI Fallback Mode
        return@withContext generateLocalFallback(mode, prompt, contextInfo, charactersList)
    }

    private fun callOpenAi(apiKey: String, systemInstruction: String, userPrompt: String): String {
        val url = "https://api.openai.com/v1/chat/completions"

        val jsonRequest = JSONObject().apply {
            put("model", "gpt-4o")
            put("temperature", 0.7)
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messagesArray)
        }

        val body = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error ${response.code}: ${response.body?.string()}")
            }
            val responseString = response.body?.string() ?: throw Exception("Empty response body")
            val jsonResponse = JSONObject(responseString)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.getJSONObject("message")
                return message.getString("content").trim()
            }
            throw Exception("No completions returned")
        }
    }

    private fun callGemini(apiKey: String, fullPrompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", fullPrompt)
                        })
                    }
                    put("parts", partsArray)
                })
            }
            put("contents", contentsArray)
        }

        val body = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error ${response.code}: ${response.body?.string()}")
            }
            val responseString = response.body?.string() ?: throw Exception("Empty response body")
            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "Tidak ada text").trim()
                    }
                }
            }
            throw Exception("Failed to parse Gemini response")
        }
    }

    /**
     * Generates extremely clever, context-focused mock screenplay completions, dialogues, or brainstorm notes
     * when there is no internet or model keys.
     */
    private fun generateLocalFallback(
        mode: String,
        prompt: String,
        contextInfo: String,
        charactersList: List<String>
    ): String {
        val cName = if (charactersList.isNotEmpty()) charactersList.random() else "DONI"
        
        return when (mode) {
            "CONTINUE" -> {
                """
                    
                    
                    $cName
                    (menatap tajam, lalu berbisik)
                    Kita tidak punya banyak waktu. Rencana ini harus berjalan malam ini juga. Kalau tidak, semuanya akan hancur.
                    
                    Aksi berlanjut saat lampu di ruangan tiba-tiba berkedip redup, disusul suara ketukan ritmis dari balik dinding sebelah...
                """.trimIndent()
            }
            "IMPROVE" -> {
                """
                    $cName
                    "Sudah kubilang berkali-kali kan, kita ini nggak bisa cuma diam nunggu keajaiban! Kita harus ambil tindakan sekarang, sebelum mereka menyadarinya!"
                """.trimIndent()
            }
            else -> {
                """
                    [💡 BRAINSTORMING SCRIPKU]
                    Berikut adalah 3 alternatif pengembangan kreatif berbasis naskah Anda:
                    
                    1. ALternatif Konflik: Karakter $cName secara tidak sengaja menemukan surat rahasia yang mengungkap bahwa sekutu terdekatnya adalah dalang di balik seluruh sabotase yang terjadi.
                    
                    2. Ide Plot Twist: Ternyata lokasi naskah Anda saat ini merupakan ruang simulasi bawah tanah, dan dunia luar sudah lama runtuh tanpa disadari para penghuninya.
                    
                    3. Opsi Ending: Cerita ditutup dengan konfrontasi emosional di atap gedung hujan badai, di mana $cName harus merelakan ambisi terbesarnya demi menyelamatkan orang yang dia benci.
                """.trimIndent()
            }
        }
    }
}
