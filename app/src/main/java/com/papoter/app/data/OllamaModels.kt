package com.papoter.app.data

import com.google.gson.annotations.SerializedName

data class OllamaTagResponse(
    val models: List<OllamaModel>
)

data class OllamaModel(
    val name: String,
    val model: String? = null,
    val modified_at: String? = null,
    val size: Long? = null,
    val digest: String? = null,
    val details: ModelDetails? = null
)

data class ModelDetails(
    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,
    val parameter_size: String? = null,
    val quantization_level: String? = null
)

data class OllamaChatRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val stream: Boolean = false,
    val tools: List<ToolSchema>? = null
)

data class ChatMessagePayload(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

data class OllamaChatResponse(
    val message: ChatMessagePayload? = null,
    val done: Boolean = false,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val eval_count: Int? = null,
    val error: String? = null
)

data class ToolSchema(
    val type: String = "function",
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String>
)

data class ToolProperty(
    val type: String,
    val description: String
)

data class ServerHealthStatus(
    val isOnline: Boolean,
    val modelCount: Int = 0,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)
