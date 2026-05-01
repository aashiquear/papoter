package com.papoter.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "papoter_prefs")

class OllamaRepository(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).conversationDao()

    companion object {
        @Volatile
        private var apiService: OllamaApiService? = null

        val SERVER_URL = stringPreferencesKey("server_url")
        val USER_NAME = stringPreferencesKey("user_name")
        val CURRENT_MODEL = stringPreferencesKey("current_model")
        val CURRENT_CONVERSATION = stringPreferencesKey("current_conversation")
        val IS_FIRST_LAUNCH = androidx.datastore.preferences.core.booleanPreferencesKey("is_first_launch")

        const val SYSTEM_PROMPT = "You are a helpful assistant. Keep your responses brief, casual, and conversational — like texting a friend. Only go into detail if the user explicitly asks for it. Avoid long paragraphs unless requested. Use short sentences and a relaxed tone."
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH] ?: true
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val currentModel: Flow<String?> = context.dataStore.data.map { it[CURRENT_MODEL] }

    fun getApiService(): OllamaApiService? {
        return apiService
    }

    suspend fun initializeApi(url: String) {
        var normalizedUrl = url.trim()
        if (!normalizedUrl.endsWith("/")) normalizedUrl += "/"
        if (!normalizedUrl.startsWith("http")) normalizedUrl = "http://$normalizedUrl"
        apiService = ApiClient.create(normalizedUrl)
        context.dataStore.edit { it[SERVER_URL] = normalizedUrl }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun saveCurrentModel(model: String) {
        context.dataStore.edit { it[CURRENT_MODEL] = model }
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[IS_FIRST_LAUNCH] = false }
    }

    suspend fun checkServerHealth(): ServerHealthStatus = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext ServerHealthStatus(false, errorMessage = "Not configured")
        val startTime = System.currentTimeMillis()
        try {
            val response = withTimeoutOrNull(5000) { service.listModels() }
            val latency = System.currentTimeMillis() - startTime
            if (response?.isSuccessful == true) {
                ServerHealthStatus(true, response.body()?.models?.size ?: 0, latency)
            } else {
                ServerHealthStatus(false, errorMessage = "Server returned ${response?.code()}")
            }
        } catch (e: Exception) {
            ServerHealthStatus(false, errorMessage = e.message ?: "Unknown error")
        }
    }

    suspend fun fetchModels(): List<OllamaModel> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext emptyList()
        try {
            val response = service.listModels()
            if (response.isSuccessful) response.body()?.models ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendChatMessage(
        messages: List<ChatMessagePayload>,
        model: String,
        tools: List<ToolSchema>? = null
    ): Result<OllamaChatResponse> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext Result.failure(Exception("API not configured"))
        try {
            val request = OllamaChatRequest(model, messages, false, tools)
            val response = service.chat(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChatMessage(
        messages: List<ChatMessagePayload>,
        model: String,
        tools: List<ToolSchema>? = null
    ): Flow<StreamingChunk> = flow {
        val service = apiService ?: throw Exception("API not configured")
        val request = OllamaChatRequest(model, messages, true, tools)
        val responseBody = service.chatStream(request)
        val gson = com.google.gson.Gson()
        var fullContent = ""
        var finalResponse: OllamaChatResponse? = null

        responseBody.use { body ->
            body.byteStream().bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line: String ->
                    if (line.isBlank()) return@forEach
                    try {
                        val chunk = gson.fromJson(line, OllamaChatResponse::class.java)
                        val delta = chunk.message?.content ?: ""
                        fullContent += delta
                        if (chunk.done == true) {
                            finalResponse = chunk.copy(message = chunk.message?.copy(content = fullContent))
                            emit(StreamingChunk(fullContent, done = true, response = finalResponse))
                        } else {
                            emit(StreamingChunk(fullContent, done = false))
                        }
                    } catch (_: Exception) {
                        // ignore malformed lines
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getAllConversations(): Flow<List<Conversation>> = dao.getAllConversations()
    fun getMessages(conversationId: String): Flow<List<Message>> = dao.getMessagesForConversation(conversationId)

    suspend fun createConversation(modelName: String): Conversation {
        val id = UUID.randomUUID().toString()
        val conv = Conversation(id = id, title = "New Chat", modelName = modelName)
        dao.insertConversation(conv)
        context.dataStore.edit { it[CURRENT_CONVERSATION] = id }
        return conv
    }

    suspend fun saveMessage(message: Message) {
        dao.insertMessage(message)
    }

    suspend fun updateConversation(conversation: Conversation) {
        dao.insertConversation(conversation.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(conversation: Conversation) {
        dao.deleteMessagesForConversation(conversation.id)
        dao.deleteConversation(conversation)
    }

    suspend fun getCurrentConversationId(): String? {
        return context.dataStore.data.first()[CURRENT_CONVERSATION]
    }

    suspend fun setCurrentConversation(id: String) {
        context.dataStore.edit { it[CURRENT_CONVERSATION] = id }
    }

    suspend fun generateTitle(model: String, content: String): String = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext "Chat"
        try {
            val messages = listOf(
                ChatMessagePayload("system", SYSTEM_PROMPT),
                ChatMessagePayload("user", "Summarize the following user message into a very short chat title (max 4 words). Reply with ONLY the title, no quotes.\n\n$content")
            )
            val request = OllamaChatRequest(model, messages, false)
            val response = service.chat(request)
            response.body()?.message?.content?.trim()?.takeIf { it.isNotBlank() } ?: "Chat"
        } catch (e: Exception) {
            "Chat"
        }
    }
}

data class StreamingChunk(
    val content: String,
    val done: Boolean,
    val response: OllamaChatResponse? = null
)
