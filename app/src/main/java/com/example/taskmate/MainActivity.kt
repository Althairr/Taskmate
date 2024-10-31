package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    // Helper function to replace the current fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // Function to save the last visited page
    private fun saveLastVisitedPage(pageName: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", 0)
        val editor = sharedPreferences.edit()
        editor.putString("last_page", pageName)
        editor.apply()
    }
}