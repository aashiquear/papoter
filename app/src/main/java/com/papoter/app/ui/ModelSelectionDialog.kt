package com.papoter.app.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
import com.papoter.app.data.OllamaModel

class ModelSelectionDialog {
    companion object {
        fun show(
            context: Context,
            models: List<OllamaModel>,
            currentModel: String,
            onModelSelected: (String) -> Unit
        ) {
            val items = models.map { it.name }.toTypedArray()
            val currentIndex = items.indexOf(currentModel).coerceAtLeast(0)

            AlertDialog.Builder(context)
                .setTitle("Choose a Model")
                .setSingleChoiceItems(ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, items), currentIndex) { _, _ -> }
                .setPositiveButton("Select") { dialog, _ ->
                    val listView = (dialog as AlertDialog).listView
                    val pos = listView.checkedItemPosition
                    if (pos >= 0) onModelSelected(items[pos])
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
