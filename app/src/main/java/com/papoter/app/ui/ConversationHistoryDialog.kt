package com.papoter.app.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.papoter.app.R
import com.papoter.app.data.Conversation
import java.text.SimpleDateFormat
import java.util.*

class ConversationHistoryDialog {
    companion object {
        fun show(
            context: Context,
            conversations: List<Conversation>,
            onSelect: (Conversation) -> Unit,
            onDelete: (Conversation) -> Unit
        ) {
            if (conversations.isEmpty()) {
                AlertDialog.Builder(context)
                    .setTitle("History")
                    .setMessage("No conversations yet. Start chatting!")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            lateinit var dialog: AlertDialog

            val adapter = object : ArrayAdapter<Conversation>(
                context, R.layout.item_conversation, conversations
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context)
                        .inflate(R.layout.item_conversation, parent, false)
                    val conv = getItem(position) ?: return view
                    view.findViewById<TextView>(R.id.text_title).text = conv.title
                    view.findViewById<TextView>(R.id.text_meta).text =
                        "${conv.modelName} • ${formatDate(conv.updatedAt)}"
                    view.findViewById<ImageButton>(R.id.button_delete).setOnClickListener {
                        AlertDialog.Builder(context)
                            .setTitle("Delete conversation?")
                            .setMessage("'${conv.title}' will be gone forever.")
                            .setPositiveButton("Delete") { _, _ ->
                                onDelete(conv)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Keep", null)
                            .show()
                    }
                    return view
                }
            }

            dialog = AlertDialog.Builder(context)
                .setTitle("Conversations")
                .setAdapter(adapter) { _, position ->
                    onSelect(conversations[position])
                }
                .setPositiveButton("New Chat") { _, _ -> onSelect(Conversation("", "", "")) }
                .setNegativeButton("Close", null)
                .create()

            dialog.show()
        }

        private fun formatDate(ts: Long): String {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return sdf.format(Date(ts))
        }
    }
}
