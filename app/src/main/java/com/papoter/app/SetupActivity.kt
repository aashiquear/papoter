package com.papoter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.papoter.app.data.OllamaRepository
import com.papoter.app.databinding.ActivitySetupBinding
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var repository: OllamaRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Some permissions denied. You can enable them later in Settings.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = OllamaRepository(this)

        binding.buttonPermissions.setOnClickListener {
            requestNeededPermissions()
        }

        binding.buttonTestConnection.setOnClickListener {
            testConnection()
        }

        binding.buttonFinish.setOnClickListener {
            finishSetup()
        }

        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testConnection() {
        val url = binding.editServerUrl.text.toString().trim()
        if (url.isBlank()) {
            binding.editServerUrl.error = "Enter server URL"
            return
        }
        lifecycleScope.launch {
            binding.buttonTestConnection.isEnabled = false
            binding.buttonTestConnection.text = "Checking..."
            try {
                repository.initializeApi(url)
                val health = repository.checkServerHealth()
                if (health.isOnline) {
                    binding.textConnectionStatus.text = "Online! Found ${health.modelCount} models. Latency: ${health.latencyMs}ms"
                    binding.textConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    binding.textConnectionStatus.text = "Offline: ${health.errorMessage}"
                    binding.textConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            } catch (e: Exception) {
                binding.textConnectionStatus.text = "Error: ${e.message}"
                binding.textConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
            binding.buttonTestConnection.isEnabled = true
            binding.buttonTestConnection.text = "Test Connection"
        }
    }

    private fun finishSetup() {
        val name = binding.editUserName.text.toString().trim()
        val url = binding.editServerUrl.text.toString().trim()
        if (name.isBlank()) {
            binding.editUserName.error = "Enter your name"
            return
        }
        if (url.isBlank()) {
            binding.editServerUrl.error = "Enter server URL"
            return
        }
        lifecycleScope.launch {
            repository.saveUserName(name)
            if (repository.getApiService() == null) {
                repository.initializeApi(url)
            }
            repository.setFirstLaunchComplete()
            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
            finish()
        }
    }
}
