package com.papoter.app.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WebSearchTool {
    // Using DuckDuckGo HTML search as a simple no-API-key approach
    // In production, replace with a proper search API like SerpAPI or Google Custom Search
    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://html.duckduckgo.com/html/?q=$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            // Very simple parsing of results
            val results = mutableListOf<String>()
            val pattern = Regex("class=\"result__a\"[^>]*>([^<]+)")
            val snippetPattern = Regex("class=\"result__snippet\"[^>]*>([^<]+)")

            val titles = pattern.findAll(response).map { it.groupValues[1].trim() }.take(3).toList()
            val snippets = snippetPattern.findAll(response).map { it.groupValues[1].trim() }.take(3).toList()

            for (i in titles.indices) {
                results.add("${titles.getOrNull(i) ?: ""}: ${snippets.getOrNull(i) ?: ""}")
            }

            if (results.isEmpty()) {
                "No quick results found for '$query'. Try asking me directly!"
            } else {
                "Search results for '$query':\n${results.joinToString("\n")}"
            }
        } catch (e: Exception) {
            "Search unavailable right now. Error: ${e.message}"
        }
    }
}
