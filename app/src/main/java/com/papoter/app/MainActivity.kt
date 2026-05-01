package com.papoter.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.papoter.app.data.OllamaRepository
import com.papoter.app.databinding.ActivityMainBinding
import com.papoter.app.tools.TimeDateTool
import com.papoter.app.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var repository: OllamaRepository

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFilePick(it) }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImagePick(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "papoter"

        repository = OllamaRepository(this)

        setupGreeting()
        setupRecyclerView()
        setupInput()
        setupStatusObserver()
        setupMessagesObserver()
        setupStreamingObserver()
        restoreServerAndModel()

        lifecycleScope.launch {
            viewModel.conversations.collect { convs ->
                if (convs.isEmpty() && viewModel.currentConversationId.value == null) {
                    viewModel.newConversation()
                }
            }
        }
    }

    private fun restoreServerAndModel() {
        lifecycleScope.launch {
            val savedUrl = repository.serverUrl.first()
            if (!savedUrl.isNullOrBlank()) {
                repository.initializeApi(savedUrl)
                viewModel.checkHealth()

                val savedModel = repository.currentModel.first()
                if (!savedModel.isNullOrBlank()) {
                    viewModel.refreshModels()
                } else {
                    val models = repository.fetchModels()
                    if (models.isNotEmpty()) {
                        repository.saveCurrentModel(models.first().name)
                        viewModel.refreshModels()
                    }
                }
            }
        }
    }

    private fun setupGreeting() {
        lifecycleScope.launch {
            val name = repository.userName.first()
            binding.textGreeting.text = TimeDateTool.getTimeOfDayGreeting(name)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupInput() {
        binding.buttonSend.setOnClickListener {
            val text = binding.editMessage.text.toString().trim()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.editMessage.text?.clear()
            }
        }

        binding.buttonAttach.setOnClickListener {
            showAttachmentMenu()
        }
    }

    private fun showAttachmentMenu() {
        val options = arrayOf("Image", "File (PDF/Text)", "Web Search")
        AlertDialog.Builder(this)
            .setTitle("Attach")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> pickFileLauncher.launch("*/*")
                    2 -> showQuickSearch()
                }
            }
            .show()
    }

    private fun showQuickSearch() {
        val input = android.widget.EditText(this).apply {
            hint = "Search query"
        }
        AlertDialog.Builder(this)
            .setTitle("Quick Search")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    viewModel.sendMessage("Search the web for: $query")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleFilePick(uri: Uri) {
        viewModel.uploadFile(uri) { content ->
            binding.editMessage.setText("Summarize this document:\n\n$content")
            Toast.makeText(this, "File loaded into message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImagePick(uri: Uri) {
        viewModel.uploadImage(uri) { base64 ->
            if (base64 != null) {
                viewModel.setImageAttachment(base64)
                Toast.makeText(this, "Image attached", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to encode image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupStatusObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statusText.collect { text ->
                    binding.textStatus.text = text
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collect { status ->
                    binding.progressStatus.visibility = if (status == ChatStatus.IDLE) android.view.View.INVISIBLE else android.view.View.VISIBLE
                }
            }
        }
    }

    private fun setupMessagesObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { messages ->
                    chatAdapter.clearStreamingText()
                    chatAdapter.submitList(messages) {
                        binding.recyclerChat.post {
                            binding.recyclerChat.scrollToPosition(messages.size - 1)
                        }
                    }
                }
            }
        }
    }

    private fun setupStreamingObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.streamingText.collect { text ->
                    if (text.isNotEmpty()) {
                        chatAdapter.setStreamingText(text)
                        binding.recyclerChat.post {
                            binding.recyclerChat.scrollToPosition(chatAdapter.currentList.size - 1)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // Show icons in the overflow menu
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            try {
                val method = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.java)
                method.isAccessible = true
                method.invoke(menu, true)
            } catch (_: Exception) {}
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                showHistory()
                true
            }
            R.id.action_model -> {
                showModelSelection()
                true
            }
            R.id.action_health -> {
                viewModel.checkHealth()
                lifecycleScope.launch {
                    val health = viewModel.serverHealth.first { it != null }
                    health?.let {
                        val msg = if (it.isOnline) {
                            "Server is online!\nModels: ${it.modelCount}\nLatency: ${it.latencyMs}ms"
                        } else {
                            "Server is offline.\n${it.errorMessage}"
                        }
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Server Health")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
                true
            }
            R.id.action_tokens -> {
                val stats = viewModel.tokenStats.value
                TokenCounterDialog.show(
                    this,
                    stats.lastPromptTokens,
                    stats.lastResponseTokens,
                    stats.averageTokens,
                    stats.totalTokens
                )
                true
            }
            R.id.action_new_chat -> {
                viewModel.newConversation()
                Toast.makeText(this, "Fresh chat started!", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_delete_chat -> {
                val convId = viewModel.conversations.value.find { c ->
                    c.id == viewModel.currentConversationId.value
                }
                if (convId != null) {
                    AlertDialog.Builder(this)
                        .setTitle("Delete this chat?")
                        .setPositiveButton("Delete") { _, _ -> viewModel.deleteConversation(convId) }
                        .setNegativeButton("Keep", null)
                        .show()
                }
                true
            }
            R.id.action_settings -> {
                showSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHistory() {
        ConversationHistoryDialog.show(
            this,
            viewModel.conversations.value,
            onSelect = { conv ->
                if (conv.id.isBlank()) {
                    viewModel.newConversation()
                } else {
                    viewModel.loadConversation(conv.id)
                }
            },
            onDelete = { conv -> viewModel.deleteConversation(conv) }
        )
    }

    private fun showModelSelection() {
        lifecycleScope.launch {
            val models = repository.fetchModels()
            if (models.isEmpty()) {
                Toast.makeText(this@MainActivity, "No models found. Check server health.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            ModelSelectionDialog.show(
                this@MainActivity,
                models,
                viewModel.currentModel.value
            ) { model ->
                lifecycleScope.launch {
                    repository.saveCurrentModel(model)
                    Toast.makeText(this@MainActivity, "Switched to $model", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSettings() {
        val urlInput = android.widget.EditText(this).apply {
            lifecycleScope.launch {
                setText(repository.serverUrl.first() ?: "")
            }
            hint = "http://192.168.1.5:11434"
        }
        AlertDialog.Builder(this)
            .setTitle("Server Settings")
            .setMessage("Update your Ollama server URL:")
            .setView(urlInput)
            .setPositiveButton("Save") { _, _ ->
                lifecycleScope.launch {
                    repository.initializeApi(urlInput.text.toString())
                    Toast.makeText(this@MainActivity, "Server updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Test") { _, _ ->
                lifecycleScope.launch {
                    repository.initializeApi(urlInput.text.toString())
                    val health = repository.checkServerHealth()
                    Toast.makeText(
                        this@MainActivity,
                        if (health.isOnline) "Online!" else "Offline: ${health.errorMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
