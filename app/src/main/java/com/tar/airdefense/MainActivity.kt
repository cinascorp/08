package com.tar.airdefense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tar.airdefense.databinding.ActivityMainBinding
import com.tar.airdefense.ui.viewmodel.MainViewModel
import com.tar.airdefense.utils.LocaleHelper
import com.tar.airdefense.utils.PermissionHelper
import timber.log.Timber

/**
 * Main Activity for TAR Air Defense System
 * Provides navigation and real-time air surveillance monitoring
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.i("All required permissions granted")
            viewModel.onPermissionsGranted()
        } else {
            Timber.w("Some permissions were denied")
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set locale based on user preference
        LocaleHelper.setLocale(this)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Setup navigation
        setupNavigation()
        
        // Setup UI
        setupUI()
        
        // Check permissions
        checkPermissions()
        
        // Observe ViewModel
        observeViewModel()
        
        Timber.i("MainActivity created successfully")
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
        
        // Setup action bar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_surveillance,
                R.id.navigation_threats,
                R.id.navigation_command,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }
    
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Setup status indicators
        binding.statusIndicator.setOnClickListener {
            viewModel.refreshSystemStatus()
        }
        
        // Setup emergency button
        binding.emergencyButton.setOnClickListener {
            showEmergencyDialog()
        }
    }
    
    private fun checkPermissions() {
        if (PermissionHelper.hasAllPermissions(this, requiredPermissions)) {
            Timber.i("All permissions already granted")
            viewModel.onPermissionsGranted()
        } else {
            Timber.i("Requesting permissions")
            permissionLauncher.launch(requiredPermissions)
        }
    }
    
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_explanation))
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    private fun showEmergencyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.emergency_alert))
            .setMessage(getString(R.string.emergency_confirmation))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                viewModel.triggerEmergencyAlert()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun observeViewModel() {
        viewModel.systemStatus.observe(this) { status ->
            updateStatusIndicator(status)
        }
        
        viewModel.threatLevel.observe(this) { level ->
            updateThreatLevel(level)
        }
        
        viewModel.emergencyAlert.observe(this) { alert ->
            if (alert != null) {
                showEmergencyNotification(alert)
            }
        }
    }
    
    private fun updateStatusIndicator(status: String) {
        binding.statusIndicator.text = status
        // Update color based on status
        when {
            status.contains("ONLINE", ignoreCase = true) -> {
                binding.statusIndicator.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_green_dark)
                )
            }
            status.contains("WARNING", ignoreCase = true) -> {
                binding.statusIndicator.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                )
            }
            status.contains("ERROR", ignoreCase = true) -> {
                binding.statusIndicator.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)
                )
            }
        }
    }
    
    private fun updateThreatLevel(level: Int) {
        binding.threatLevelIndicator.text = getString(R.string.threat_level, level)
        binding.threatLevelIndicator.setTextColor(
            when (level) {
                1 -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
                2 -> ContextCompat.getColor(this, android.R.color.holo_blue_dark)
                3 -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                4 -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
                5 -> ContextCompat.getColor(this, android.R.color.holo_purple)
                else -> ContextCompat.getColor(this, android.R.color.darker_gray)
            }
        )
    }
    
    private fun showEmergencyNotification(alert: String) {
        Snackbar.make(
            binding.root,
            alert,
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.dismiss)) {
            // Dismiss action
        }.show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language -> {
                showLanguageDialog()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf("فارسی", "English", "Русский", "Svenska")
        val currentLanguage = LocaleHelper.getCurrentLanguage(this)
        val currentIndex = when (currentLanguage) {
            "fa" -> 0
            "en" -> 1
            "ru" -> 2
            "sv" -> 3
            else -> 1
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languages, currentIndex) { _, which ->
                val languageCode = when (which) {
                    0 -> "fa"
                    1 -> "en"
                    2 -> "ru"
                    3 -> "sv"
                    else -> "en"
                }
                LocaleHelper.setLocale(this, languageCode)
                recreate()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.about_tar))
            .setMessage(getString(R.string.about_description))
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.help))
            .setMessage(getString(R.string.help_description))
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.onActivityResumed()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.onActivityPaused()
    }
}