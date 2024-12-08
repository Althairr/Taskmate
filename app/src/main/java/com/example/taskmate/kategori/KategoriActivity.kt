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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class KategoriActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: KategoriAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori)

        // Handle the "Batal" (Back) button click
        val batal = findViewById<TextView>(R.id.batal)
        batal.setOnClickListener {
            finish() // This will take the user back to the previous activity
        }

        // Setup RecyclerView for Categories
        recyclerView = findViewById(R.id.recycler_view_categories)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch categories from Firestore for the current user
        fetchCategories()
    }

    // Function to fetch categories from Firestore by user ID
    private fun fetchCategories() {
        db.collection("categories") // Assuming categories are stored in the "categories" collection
            .whereEqualTo("userId", currentUserId) // Filter categories by user ID
            .get()
            .addOnSuccessListener { result ->
                val categories = mutableListOf<String>()
                for (document in result) {
                    val category = document.getString("name") // Assuming each category has a "name" field
                    category?.let { categories.add(it) }
                }

                // If no categories found, add a default category
                if (categories.isEmpty()) {
                    categories.add("No Categories Available")
                }

                // Set up adapter with the fetched categories
                categoryAdapter = KategoriAdapter(categories) { view, position ->
                    showPopup(view, position)
                }
                recyclerView.adapter = categoryAdapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching categories: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to show the popup menu for category actions (edit/delete)
    private fun showPopup(view: View, position: Int) {
        val popup = PopupMenu(this, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.edit_kategori, popup.menu)

        // Add click listener for the "Edit" option in the popup
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    // Show the category edit dialog
                    showEditCategoryDialog(position)
                    true
                }
                R.id.action_delete -> {
                    // Handle the "Delete" action for the selected category
                    Toast.makeText(this, "Delete clicked for item $position", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }
    private fun showEditCategoryDialog(position: Int) {
        val category = categoryAdapter.getCategories()[position]  // Get the current category
        val dialogView = layoutInflater.inflate(R.layout.dialog_ubah_kategori, null) // Use your provided XML layout
        val editKategoriEditText = dialogView.findViewById<EditText>(R.id.edit_kategori)
        val batalButton = dialogView.findViewById<Button>(R.id.batal_button)
        val selesaiButton = dialogView.findViewById<Button>(R.id.selesai_button)

        // Set the current category name in the EditText field
        editKategoriEditText.setText(category)

        // Create and show the dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Handle "Batal" button click (dismiss dialog)
        batalButton.setOnClickListener {
            dialog.dismiss()
        }

        // Handle "Selesai" button click (update category in Firestore)
        selesaiButton.setOnClickListener {
            val newCategoryName = editKategoriEditText.text.toString()

            if (newCategoryName.isNotEmpty()) {
                updateCategoryInFirestore(position, newCategoryName)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateCategoryInFirestore(position: Int, newCategoryName: String) {
        val categoryRef = db.collection("categories")
            .whereEqualTo("userId", currentUserId) // Assuming userId is stored in Firestore
            .get()
            .addOnSuccessListener { result ->
                val document = result.documents[position] // Get the document for the selected category
                val categoryId = document.id // Get the category document ID

                // Update the category name
                db.collection("categories").document(categoryId)
                    .update("name", newCategoryName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
                        fetchCategories() // Refresh the categories
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error updating category: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching category document: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



}
