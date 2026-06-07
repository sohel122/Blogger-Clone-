package com.example.data

import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.BuildConfig

// --- Moshi Models for Gemini API ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<MoshiContent>,
    val generationConfig: MoshiGenerationConfig? = null,
    val systemInstruction: MoshiContent? = null
)

@JsonClass(generateAdapter = true)
data class MoshiContent(
    val parts: List<MoshiPart>
)

@JsonClass(generateAdapter = true)
data class MoshiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class MoshiGenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<MoshiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class MoshiCandidate(
    val content: MoshiContent? = null
)

// --- Retrofit Endpoint Definition ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Network Client Configured with 60-second Timeouts ---

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Main Helper to Generate Blog-Related AI Actions ---

object GeminiHelper {
    private val TAG = "GeminiHelper"

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateContent(prompt: String, systemPrompt: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder.")
            return@withContext "Error: Gemini API Key is missing. Please add your GEMINI_API_KEY in the AI Studio Secrets Panel before executing AI content generation."
        }

        val request = GeminiRequest(
            contents = listOf(MoshiContent(parts = listOf(MoshiPart(text = prompt)))),
            generationConfig = MoshiGenerationConfig(temperature = 0.7f),
            systemInstruction = if (systemPrompt.isNotEmpty()) MoshiContent(parts = listOf(MoshiPart(text = systemPrompt))) else null
        )

        try {
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response content generated from Gemini."
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini API", e)
            val errorMessage = e.message ?: "Unknown communication fault"
            if (errorMessage.contains("403") || errorMessage.contains("API key")) {
                "Error: API Key verification failed. Ensure your Gemini API Key in the Secrets Panel is correct and authorized."
            } else {
                "Error executing AI prompt: $errorMessage"
            }
        }
    }

    suspend fun generateBlogPostDraft(title: String, category: String, keywords: String): String {
        val systemPrompt = "You are a professional, SEO-optimized blog copywriter. Write detailed, engaging articles in markdown styling with clear headings, bullet points, and well-structured concepts."
        val prompt = "Write a complete blog post titled '$title' in the category of '$category'. Ensure to weave in these topics and keywords naturally: $keywords. Start with a catchy hook and conclude with an invitation for reader comments."
        return generateContent(prompt, systemPrompt)
    }

    suspend fun generateSeoTagSuggestions(title: String, description: String): String {
        val systemPrompt = "You are a Google SEO analyst. Respond with an informative, helpful Markdown block containing meta-tag recommendations, search-friendly titles, and 5-8 relevant tags & keywords."
        val prompt = "Generate highly competitive SEO Meta Description, robotic-tag, and search keyword configurations for a blog post titled '$title' describing: $description"
        return generateContent(prompt, systemPrompt)
    }

    suspend fun generateTranslation(content: String, targetLanguage: String): String {
        val systemPrompt = "You are an expert bilingual, culturally-sensitive blog translator. Keep the exact markdown styling, headers, formatting, bullet points, and links. ONLY translate the visible text fields and content bodies into the requested target language."
        val prompt = "Translate the following blog content precisely into '$targetLanguage':\n\n$content"
        return generateContent(prompt, systemPrompt)
    }
}
