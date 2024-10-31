package com.example.taskmate.kategori

import android.os.Bundle
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.R

class KategoriActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: KategoriAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori)

        // Handle back (Batal) click
        val batal = findViewById<TextView>(R.id.batal)
        batal.setOnClickListener {
            finish() // This will take the user back to the previous activity
        }

        // Setup RecyclerView for Categories
        recyclerView = findViewById(R.id.recycler_view_categories)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Sample categories
        val categories = listOf("Machine Learning", "RTI", "Data Science", "AI", "TEST")

        // Set up adapter with the list of categories
        categoryAdapter = KategoriAdapter(categories) { view, position ->
            showPopup(view, position)
        }

        recyclerView.adapter = categoryAdapter
    }

    // Function to show popup menu
    private fun showPopup(view: View, position: Int) {
        val popup = PopupMenu(this, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.edit_kategori, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    // Show the custom dialog
                    showEditDialog()
                    true
                }
                R.id.action_delete -> {
                    Toast.makeText(this, "Hapus clicked for item $position", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ubah_kategori, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Handle button clicks in the dialog
        dialogView.findViewById<Button>(R.id.batal_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.selesai_button).setOnClickListener {
            val newCategory = dialogView.findViewById<EditText>(R.id.edit_kategori).text.toString()
            Toast.makeText(this, "Category updated to: $newCategory", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
