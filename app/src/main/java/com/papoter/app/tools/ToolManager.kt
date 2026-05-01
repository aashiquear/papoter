package com.papoter.app.tools

import android.content.Context
import android.net.Uri
import com.papoter.app.data.ToolSchema
import com.papoter.app.data.ToolFunction
import com.papoter.app.data.ToolParameters
import com.papoter.app.data.ToolProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ToolManager(private val context: Context) {

    private val availableTools = listOf(
        ToolSchema(
            function = ToolFunction(
                name = "get_current_time",
                description = "Get the current date and time.",
                parameters = ToolParameters(
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        ),
        ToolSchema(
            function = ToolFunction(
                name = "get_user_location",
                description = "Get the user's current approximate location (city/country level). Only call if the user explicitly asks about their location or weather.",
                parameters = ToolParameters(
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        ),
        ToolSchema(
            function = ToolFunction(
                name = "web_search",
                description = """Search the web for current information. Use when the user asks about: recent events, news, weather, sports scores, stock prices, current dates, or anything that might be outside your training data.

Examples of when to search:
- "What's the weather in Tokyo?"
- "Who won the game last night?"
- "Latest news about AI"
- "What happened this week?"
- "Current price of Bitcoin"
- "What time is it in London?"
- "Did anything major happen today?"
- "Show me headlines from today"
- "What's trending right now?"
- "Election results"
- "Stock market today"

If the user asks about anything dated after your knowledge cutoff, or asks for real-time data, always call this tool.""".trimIndent(),
                parameters = ToolParameters(
                    properties = mapOf(
                        "query" to ToolProperty("string", "The search query")
                    ),
                    required = listOf("query")
                )
            )
        )
    )

    fun getToolSchemas(): List<ToolSchema> = availableTools

    suspend fun executeTool(name: String, arguments: String?): String = withContext(Dispatchers.IO) {
        when (name) {
            "get_current_time" -> TimeDateTool.execute()
            "get_user_location" -> LocationTool.getLocation(context)
            "web_search" -> {
                val query = try {
                    JSONObject(arguments ?: "{}").optString("query", "")
                } catch (e: Exception) { "" }
                if (query.isBlank()) "No search query provided." else WebSearchTool.search(query)
            }
            else -> "Unknown tool: $name"
        }
    }

    /**
     * Detects if the user's message implies a tool should be used,
     * executes it proactively, and returns the result (or null if no tool matched).
     */
    suspend fun detectAndExecute(userMessage: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val lower = userMessage.lowercase()

        when {
            lower.contains("what time") || lower.contains("current time") || lower.contains("what is the time") || lower.contains("time is it") -> {
                val result = TimeDateTool.execute()
                "get_current_time" to result
            }
            lower.contains("where am i") || lower.contains("my location") || lower.contains("current location") || lower.contains("weather") -> {
                val result = LocationTool.getLocation(context)
                "get_user_location" to result
            }
            lower.startsWith("search ") || lower.startsWith("web search") || lower.startsWith("google ") || lower.contains("search the web") -> {
                val query = userMessage.removePrefix("search ").removePrefix("web search ").removePrefix("google ").removePrefix("search the web for ").removePrefix("search the web ").trim()
                if (query.isBlank()) null else {
                    val result = WebSearchTool.search(query)
                    "web_search" to result
                }
            }
            // Expanded proactive detection for news, current events, sports, stocks, etc.
            lower.contains("news") || lower.contains("headlines") || lower.contains("breaking") ||
            lower.contains("latest") || lower.contains("recent") || lower.contains("update") ||
            lower.contains("today") || lower.contains("this week") || lower.contains("this month") ||
            lower.contains("who won") || lower.contains("score") || lower.contains("match result") ||
            lower.contains("election") || lower.contains("war") || lower.contains("outbreak") ||
            lower.contains("stock price") || lower.contains("price of") || lower.contains("bitcoin") ||
            lower.contains("crypto") || lower.contains("market") ||
            lower.contains("2024") || lower.contains("2025") || lower.contains("2026") ||
            lower.contains("happened") || lower.contains("going on") || lower.contains("what's new") ||
            lower.contains("trending") || lower.contains("viral") ||
            lower.contains("sports") || lower.contains("game last night") || lower.contains("champion") ||
            lower.contains("premier league") || lower.contains("nba") || lower.contains("nfl") -> {
                val result = WebSearchTool.search(userMessage)
                "web_search" to result
            }
            else -> null
        }
    }

    // For manual invocation from UI
    suspend fun doWebSearch(query: String): String = WebSearchTool.search(query)
    suspend fun readFile(uri: Uri): String = FileUploadTool.summarizeFile(context, uri)
    suspend fun encodeImage(uri: Uri): String? = ImageUploadTool.uriToBase64(context, uri)
}
