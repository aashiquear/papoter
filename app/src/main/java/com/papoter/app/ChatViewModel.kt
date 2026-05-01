package com.papoter.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.papoter.app.data.*
import com.papoter.app.tools.ToolManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OllamaRepository(application)
    private val toolManager = ToolManager(application)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _status = MutableStateFlow(ChatStatus.IDLE)
    val status: StateFlow<ChatStatus> = _status.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    private val _serverHealth = MutableStateFlow<ServerHealthStatus?>(null)
    val serverHealth: StateFlow<ServerHealthStatus?> = _serverHealth.asStateFlow()

    private val _availableModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val availableModels: StateFlow<List<OllamaModel>> = _availableModels.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _tokenStats = MutableStateFlow(TokenStats())
    val tokenStats: StateFlow<TokenStats> = _tokenStats.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private var pendingImageBase64: String? = null

    init {
        viewModelScope.launch {
            repository.currentModel.collect { model ->
                _currentModel.value = model ?: ""
            }
        }
        viewModelScope.launch {
            repository.getAllConversations().collect { list ->
                _conversations.value = list
            }
        }
        viewModelScope.launch {
            repository.getCurrentConversationId()?.let { id ->
                loadConversation(id)
            }
        }
    }

    fun setStatus(newStatus: ChatStatus) {
        _status.value = newStatus
        _statusText.value = getRandomFunnyText(newStatus)
    }

    private fun getRandomFunnyText(status: ChatStatus): String {
        val random = Random()
        return when (status) {
            ChatStatus.THINKING -> listOf(
                "pondering life's mysteries...",
                "brewing thoughts...",
                "scratching digital head...",
                "consulting the oracle...",
                "connecting the neurons..."
            ).random()
            ChatStatus.SEARCHING -> listOf(
                "digging through the web...",
                "asking Google nicely...",
                "rummaging online...",
                "surfing the net...",
                "interrogating the internet..."
            ).random()
            ChatStatus.RESPONDING -> listOf(
                "finding the right words...",
                "typing with tiny fingers...",
                "assembling wisdom...",
                "crafting a reply...",
                "polishing the prose..."
            ).random()
            ChatStatus.IDLE -> listOf(
                "waiting for your thoughts...",
                "ear perked up...",
                "ready to natter...",
                "at your service...",
                "silence is awkward..."
            ).random()
            ChatStatus.UPLOADING -> listOf(
                "peeking at your file...",
                "digesting the document...",
                "eyeballing pixels...",
                "unpacking your upload..."
            ).random()
        }
    }

    fun sendMessage(content: String) {
        val convId = _currentConversationId.value ?: return
        viewModelScope.launch {
            val userMsg = Message(
                conversationId = convId,
                role = "user",
                content = content,
                imageBase64 = pendingImageBase64
            )
            repository.saveMessage(userMsg)
            refreshMessages(convId)

            // Update title on first message
            val conv = _conversations.value.find { it.id == convId }
            if (conv != null && conv.messageCount == 0) {
                val title = repository.generateTitle(_currentModel.value, content)
                repository.updateConversation(conv.copy(title = title, messageCount = 1))
            } else if (conv != null) {
                repository.updateConversation(conv.copy(messageCount = conv.messageCount + 1))
            }

            // Detect and execute tools proactively based on user message
            val toolResult = toolManager.detectAndExecute(content)

            setStatus(ChatStatus.THINKING)
            performChatCompletion(convId, toolResult)
        }
    }

    private suspend fun performChatCompletion(convId: String, toolResult: Pair<String, String>?) {
        val baseMsgs = _messages.value.map {
            ChatMessagePayload(it.role, it.content, it.imageBase64?.let { listOf(it) })
        }

        // Prepend system prompt for brief, casual responses
        val systemMsg = ChatMessagePayload("system", OllamaRepository.SYSTEM_PROMPT)
        var msgs = listOf(systemMsg) + baseMsgs

        // If a tool was triggered, prepend a system/tool context message before the assistant call
        msgs = when (toolResult?.first) {
            "get_current_time" -> {
                setStatus(ChatStatus.THINKING)
                msgs + ChatMessagePayload(
                    "system",
                    "The user asked about the current time. Here is the exact current time and date: ${toolResult.second}. Answer naturally based on this."
                )
            }
            "get_user_location" -> {
                setStatus(ChatStatus.THINKING)
                msgs + ChatMessagePayload(
                    "system",
                    "The user asked about their location or weather. Here is their approximate location data: ${toolResult.second}. Answer naturally based on this."
                )
            }
            "web_search" -> {
                setStatus(ChatStatus.SEARCHING)
                msgs + ChatMessagePayload(
                    "system",
                    "The user asked for current information. Here are the web search results: ${toolResult.second}. Summarize and answer based on these results. Keep it brief."
                )
            }
            else -> msgs
        }

        // Insert placeholder assistant message for streaming
        val placeholder = Message(conversationId = convId, role = "assistant", content = "")
        val currentList = _messages.value + placeholder
        _messages.value = currentList
        _streamingText.value = ""
        setStatus(ChatStatus.RESPONDING)

        var finalContent = ""
        var promptTokens = 0
        var evalTokens = 0
        var success = false

        try {
            repository.streamChatMessage(msgs, _currentModel.value).collect { chunk ->
                finalContent = chunk.content
                _streamingText.value = finalContent
                if (chunk.done) {
                    success = true
                    chunk.response?.let { resp ->
                        promptTokens = resp.prompt_eval_count ?: 0
                        evalTokens = resp.eval_count ?: 0
                    }
                }
            }
        } catch (e: Exception) {
            finalContent = "Oops, something went wrong: ${e.message}"
            success = false
        }

        // Replace placeholder with final message
        val updatedList = _messages.value.toMutableList()
        if (updatedList.isNotEmpty()) {
            updatedList[updatedList.size - 1] = Message(
                conversationId = convId,
                role = "assistant",
                content = finalContent,
                tokensUsed = evalTokens
            )
            _messages.value = updatedList
        }

        if (success) {
            _tokenStats.value = _tokenStats.value.copy(
                lastPromptTokens = promptTokens,
                lastResponseTokens = evalTokens,
                totalTokens = _tokenStats.value.totalTokens + promptTokens + evalTokens,
                messageCount = _tokenStats.value.messageCount + 1
            )
            saveAssistantMessage(convId, finalContent, evalTokens)
        } else {
            saveAssistantMessage(convId, finalContent, 0)
        }

        _streamingText.value = ""
        pendingImageBase64 = null
        setStatus(ChatStatus.IDLE)
    }

    private suspend fun saveAssistantMessage(convId: String, content: String, tokens: Int) {
        val msg = Message(conversationId = convId, role = "assistant", content = content, tokensUsed = tokens)
        repository.saveMessage(msg)
        refreshMessages(convId)
    }

    fun setImageAttachment(base64: String?) {
        pendingImageBase64 = base64
    }

    fun newConversation() {
        viewModelScope.launch {
            val conv = repository.createConversation(_currentModel.value.ifBlank { "llama2" })
            _currentConversationId.value = conv.id
            _messages.value = emptyList()
            _tokenStats.value = TokenStats()
        }
    }

    fun loadConversation(id: String) {
        viewModelScope.launch {
            _currentConversationId.value = id
            repository.setCurrentConversation(id)
            refreshMessages(id)
            // Recalc token stats
            val msgs = repository.getMessages(id).first()
            val total = msgs.sumOf { it.tokensUsed }
            _tokenStats.value = TokenStats(
                totalTokens = total,
                messageCount = msgs.size
            )
        }
    }

    private suspend fun refreshMessages(convId: String) {
        repository.getMessages(convId).first().let { list ->
            _messages.value = list
        }
    }

    fun deleteConversation(conv: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conv)
            if (_currentConversationId.value == conv.id) {
                _messages.value = emptyList()
                _currentConversationId.value = null
            }
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _serverHealth.value = repository.checkServerHealth()
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            _availableModels.value = repository.fetchModels()
        }
    }

    fun uploadFile(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            setStatus(ChatStatus.UPLOADING)
            val content = toolManager.readFile(uri)
            setStatus(ChatStatus.IDLE)
            onDone(content)
        }
    }

    fun uploadImage(uri: Uri, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            setStatus(ChatStatus.UPLOADING)
            val base64 = toolManager.encodeImage(uri)
            setStatus(ChatStatus.IDLE)
            onDone(base64)
        }
    }

    data class TokenStats(
        val lastPromptTokens: Int = 0,
        val lastResponseTokens: Int = 0,
        val totalTokens: Int = 0,
        val messageCount: Int = 0
    ) {
        val averageTokens: Int
            get() = if (messageCount > 0) totalTokens / messageCount else 0
    }
}

enum class ChatStatus {
    IDLE, THINKING, SEARCHING, RESPONDING, UPLOADING
}
