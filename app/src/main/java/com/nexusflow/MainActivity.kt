package com.nexusflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.nexusflow.databinding.ActivityMainBinding
import com.nexusflow.ui.AboutFragment
import com.nexusflow.ui.SplashFragment
import com.nexusflow.ui.logs.LogsFragment
import com.nexusflow.ui.rules.RulesFragment
import com.nexusflow.ui.theme.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications are disabled. Some actions may not show alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (savedInstanceState == null) {
            getSharedPreferences("nexus_prefs", MODE_PRIVATE).edit().putBoolean("first_run", true).apply()
            loadFragment(SplashFragment(), hideNav = true)
        }
        
        setupToolbar()
        setupThemeSelection()
        setupNavigation()
        
        checkSystemPermissions()
    }

    private fun checkSystemPermissions() {
        // 1. Check WRITE_SETTINGS (for Brightness)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "Please grant 'Modify System Settings' for Brightness", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        // 2. Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupToolbar() {
        val app = application as NexusFlowApp
        binding.engineToggle.isChecked = app.engine.globalEnabled
        binding.engineToggle.setOnCheckedChangeListener { _, isChecked ->
            app.engine.globalEnabled = isChecked
        }
    }

    private fun setupThemeSelection() {
        binding.themeSlider.isChecked = ThemeManager.isDarkMode(this)
        binding.themeSlider.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(this, isChecked)
            recreate()
        }

        binding.btnThemeFire.setOnClickListener {
            ThemeManager.setThemeFamily(this, ThemeManager.ThemeFamily.FIRE)
            recreate()
        }
        binding.btnThemeSolar.setOnClickListener {
            ThemeManager.setThemeFamily(this, ThemeManager.ThemeFamily.SOLAR)
            recreate()
        }
        binding.btnThemePastel.setOnClickListener {
            ThemeManager.setThemeFamily(this, ThemeManager.ThemeFamily.PASTEL)
            recreate()
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_rules -> {
                    loadFragment(RulesFragment())
                    true
                }
                R.id.navigation_logs -> {
                    loadFragment(LogsFragment())
                    true
                }
                R.id.navigation_about -> {
                    loadFragment(AboutFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun loadFragment(fragment: Fragment, hideNav: Boolean = false) {
        binding.bottomNav.visibility = if (hideNav) View.GONE else View.VISIBLE

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
    
    fun onGetStarted() {
        getSharedPreferences("nexus_prefs", MODE_PRIVATE).edit().putBoolean("first_run", false).apply()
        loadFragment(RulesFragment())
    }
}
