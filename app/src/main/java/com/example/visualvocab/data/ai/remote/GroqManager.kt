package com.example.visualvocab.data.ai.remote

import com.example.visualvocab.data.ai.remote.dto.GroqRequestDto
import com.example.visualvocab.data.ai.remote.dto.GroqResponseDto
import com.example.visualvocab.data.ai.remote.dto.MessageDto
import com.example.visualvocab.data.ai.remote.dto.ResponseFormatDto
import com.example.visualvocab.data.ai.remote.dto.VocabularyResultDto
import com.example.visualvocab.domain.model.SentenceDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// this class is what we use to talk to the Groq AI service.
class GroqManager(
    private val apiKey: String
) {

    // we set up the HTTP client here with some timeouts so it doesn't wait forever.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // setting up the JSON parser to be a bit more relaxed about what it sees.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    // this is the main function that asks the AI for words based on what we saw.
    suspend fun generateVocabulary(
        word: String,
        previousEnglishSentence: String?,
        difficulty: SentenceDifficulty
    ): VocabularyResultDto {
        val cleanedWord = word.trim().lowercase()

        // make sure we actually have a word to look up.
        require(cleanedWord.isNotBlank()) {
            "The selected word is empty."
        }

        // check if the API key is actually there.
        check(
            apiKey.isNotBlank() &&
                    apiKey != "YOUR_API_KEY_HERE"
        ) {
            "The Groq API key has not been configured."
        }

        // building the request to send to the AI.
        val requestBody = GroqRequestDto(
            model = MODEL_NAME,
            messages = listOf(
                MessageDto(
                    role = "system",
                    content = SYSTEM_MESSAGE
                ),
                MessageDto(
                    role = "user",
                    content = createPrompt(
                        word = cleanedWord,
                        previousSentence = previousEnglishSentence,
                        difficulty = difficulty
                    )
                )
            ),
            temperature = 0.35,
            maxTokens = 220,
            responseFormat = ResponseFormatDto(
                type = "json_object"
            )
        )

        val request = Request.Builder()
            .url(CHAT_COMPLETIONS_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(
                json.encodeToString(requestBody)
                    .toRequestBody(JSON_MEDIA_TYPE)
            )
            .build()

        // actually sending the request and waiting for the answer.
        val responseText = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                // if the server says no, we throw an error.
                if (!response.isSuccessful) {
                    val details = body
                        ?.take(300)
                        ?.takeIf { it.isNotBlank() }

                    throw IllegalStateException(
                        buildString {
                            append("Groq request failed (${response.code})")
                            if (details != null) {
                                append(": ")
                                append(details)
                            }
                        }
                    )
                }

                body ?: throw IllegalStateException(
                    "Groq returned an empty response."
                )
            }
        }

        // turn the response into something we can use in the app.
        val groqResponse =
            json.decodeFromString<GroqResponseDto>(responseText)

        val content = groqResponse.choices
            .firstOrNull()
            ?.message
            ?.content
            ?.trim()
            ?.removeMarkdownFence()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Groq returned no vocabulary content."
            )

        return try {
            json.decodeFromString<VocabularyResultDto>(content)
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Groq returned invalid vocabulary data.",
                exception
            )
        }
    }

    // this part builds the message we send to the AI so it knows what to do.
    private fun createPrompt(
        word: String,
        previousSentence: String?,
        difficulty: SentenceDifficulty
    ): String {
        val previousInstruction = previousSentence
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                "Previous English sentence: \"$it\". Generate a different sentence."
            }
            .orEmpty()

        // we tell the AI how hard the sentences should be.
        val difficultyInstruction = when (difficulty) {
            SentenceDifficulty.EASY -> """
                Difficulty: EASY.
                Use CEFR A1 language.
                Use 3 to 6 words.
                Use very common vocabulary and simple present tense.
            """.trimIndent()

            SentenceDifficulty.MEDIUM -> """
                Difficulty: MEDIUM.
                Use CEFR A2 to B1 language.
                Use 6 to 10 words.
                You may use a common adjective, preposition, or simple clause.
            """.trimIndent()

            SentenceDifficulty.HARD -> """
                Difficulty: HARD.
                Use CEFR B1 to B2 language.
                Use 9 to 15 words.
                Use more varied vocabulary and a natural, slightly complex structure.
            """.trimIndent()
        }

        return """
            Create a bilingual vocabulary result for this English object:

            $word

            $previousInstruction

            $difficultyInstruction

            Requirements:
            - Keep the English object word accurate.
            - Translate the object word into natural Spanish.
            - Create one English sentence using the object word.
            - Translate the complete English sentence into natural Spanish.
            - Keep both sentences equivalent in meaning and difficulty.
            - Do not invent an unrelated object.
            - Preserve natural Spanish articles and grammatical gender.

            Return exactly this JSON structure:

            {
              "englishWord": "$word",
              "spanishWord": "Spanish translation",
              "englishSentence": "English sentence",
              "spanishSentence": "Spanish translation"
            }
        """.trimIndent()
    }

    // sometimes the AI puts its answer in a code block, so we have to clean that up.
    private fun String.removeMarkdownFence(): String {
        return trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    companion object {
        private const val MODEL_NAME = "llama-3.1-8b-instant"
        private const val CHAT_COMPLETIONS_URL =
            "https://api.groq.com/openai/v1/chat/completions"

        private val JSON_MEDIA_TYPE =
            "application/json".toMediaType()

        private const val SYSTEM_MESSAGE =
            "You are a bilingual English and Spanish vocabulary tutor. " +
                    "Use natural neutral Spanish. Return exactly one valid JSON object. " +
                    "Do not use Markdown or add explanations outside the JSON object."
    }
}
