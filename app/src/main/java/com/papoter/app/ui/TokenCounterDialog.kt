package com.papoter.app.ui

import android.app.AlertDialog
import android.content.Context
import com.papoter.app.R

class TokenCounterDialog {
    companion object {
        fun show(
            context: Context,
            promptTokens: Int,
            responseTokens: Int,
            avgTokens: Int,
            totalTokens: Int
        ) {
            val message = buildString {
                appendLine("Last prompt: $promptTokens tokens")
                appendLine("Last response: $responseTokens tokens")
                appendLine("Average per message: $avgTokens tokens")
                appendLine("Conversation total: $totalTokens tokens")
                appendLine()
                appendLine(getFunnyMessage(totalTokens))
            }

            AlertDialog.Builder(context)
                .setTitle("Token Counter")
                .setMessage(message)
                .setPositiveButton("Cool") { _, _ -> }
                .show()
        }

        private fun getFunnyMessage(totalTokens: Int): String {
            return when {
                totalTokens < 500 -> "Light as a feather. This chat is barely a whisper."
                totalTokens < 2000 -> "Keeping it casual. Nice and tidy conversation."
                totalTokens < 5000 -> "Getting chatty! I see we're having a proper natter."
                totalTokens < 10000 -> "Wordy but worth it. Deep thoughts require deep tokens."
                totalTokens < 20000 -> "Epic novel territory. Maybe start a fresh chat soon?"
                else -> "Token tsunami! Your model might start forgetting what we said earlier."
            }
        }
    }
}
