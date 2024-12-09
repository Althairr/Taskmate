package com.example.taskmate

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.taskmate.worker.NotificationService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                startNotificationService()
            }
        } else {
            startNotificationService()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d("MainActivity", "Requesting to ignore battery optimizations.")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Log.d("MainActivity", "Battery optimizations already ignored.")
            }
        }

        // Periksa optimisasi baterai
        checkBatteryOptimization()

        // Setup Bottom Navigation and button to navigate to task
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val btnToTask = findViewById<ImageButton>(R.id.btn_add)

        // Set click listener on the button to redirect to TaskActivity
        btnToTask.setOnClickListener {
            val intent = Intent(this, TaskActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences = getSharedPreferences("app_prefs", 0)
        val lastPage = sharedPreferences.getString("last_page", null)

        if (lastPage != null) {
            when (lastPage) {
                "CalenderFragment" -> {loadFragment(CalenderFragment())
                    bottomNavigationView.selectedItemId = R.id.navigation_calender
                }
                "ArsipFragment" -> {loadFragment(ArsipFragment())
                    bottomNavigationView.selectedItemId = R.id.navigation_arsip
                }
                "SettingsFragment" -> {loadFragment(SettingsFragment())
                    bottomNavigationView.selectedItemId = R.id.navigation_settings
                }
                else -> {loadFragment(HomeFragment())
                    bottomNavigationView.selectedItemId = R.id.navigation_home
                }// Default ke HomeFragment
            }
        } else {
            loadFragment(HomeFragment()) // Jika tidak ada halaman terakhir
        }

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            var pageName: String? = null

            when (item.itemId) {
                R.id.navigation_home -> {
                    selectedFragment = HomeFragment()
                    pageName = "HomeFragment"
                }

                R.id.navigation_calender -> {
                    selectedFragment = CalenderFragment()
                    pageName = "CalenderFragment"
                }

                R.id.navigation_arsip -> {
                    selectedFragment = ArsipFragment()
                    pageName = "ArsipFragment"
                }

                R.id.navigation_settings -> {
                    selectedFragment = SettingsFragment()
                    pageName = "SettingsFragment"
                }
            }

            // Simpan halaman terakhir yang dikunjungi
            if (pageName != null) {
                saveLastVisitedPage(pageName)
            }

            selectedFragment?.let { loadFragment(it) }
            true
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d("MainActivity", "Requesting to ignore battery optimizations.")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Log.d("MainActivity", "Battery optimizations already ignored.")
            }
        }
    }

    // Helper function to replace the current fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java)
        Log.d("MainActivity", "Starting NotificationService...")
        ContextCompat.startForegroundService(this, serviceIntent)
        Log.d("MainActivity", "NotificationService started.")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted.")
                startNotificationService()
            } else {
                Log.e("MainActivity", "POST_NOTIFICATIONS permission denied.")
            }
        }
    }

    // Function to save the last visited page
    private fun saveLastVisitedPage(pageName: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", 0)
        val editor = sharedPreferences.edit()
        editor.putString("last_page", pageName)
        editor.apply()
    }
}