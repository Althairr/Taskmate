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
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var categories: MutableList<String> = mutableListOf()
    private var categoryIds: MutableList<String> = mutableListOf()

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

        fetchCategories()
    }

    private fun fetchCategories() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("categories")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                categories.clear()
                categoryIds.clear()

                for (document in querySnapshot) {
                    categories.add(document.getString("name") ?: "Unnamed Category")
                    categoryIds.add(document.id)
                }

                // Update RecyclerView with fetched categories
                categoryAdapter = KategoriAdapter(categories) { view, position ->
                    showPopup(view, position)
                }
                recyclerView.adapter = categoryAdapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch categories.", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to show popup menu
    private fun showPopup(view: View, position: Int) {
        val popup = PopupMenu(this, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.edit_kategori, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditDialog(position)
                    true
                }
                R.id.action_delete -> {
                    deleteCategory(position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDialog(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ubah_kategori, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val editText = dialogView.findViewById<EditText>(R.id.edit_kategori)
        editText.setText(categories[position]) // Set current category name

        dialogView.findViewById<Button>(R.id.batal_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.selesai_button).setOnClickListener {
            val newCategory = editText.text.toString().trim()
            if (newCategory.isNotEmpty()) {
                updateCategory(position, newCategory)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Category name cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateCategory(position: Int, newCategory: String) {
        val categoryId = categoryIds[position]

        firestore.collection("categories").document(categoryId)
            .update("name", newCategory)
            .addOnSuccessListener {
                Toast.makeText(this, "Category updated.", Toast.LENGTH_SHORT).show()
                categories[position] = newCategory
                categoryAdapter.notifyItemChanged(position)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update category.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCategory(position: Int) {
        // Kategori yang akan dihapus
        val categoryId = categoryIds[position]
        val categoryName = categories[position]

        // Hapus kategori dari Firestore
        firestore.collection("categories").document(categoryId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Category deleted.", Toast.LENGTH_SHORT).show()
                categories.removeAt(position)
                categoryIds.removeAt(position)
                categoryAdapter.notifyItemRemoved(position)

                // Update semua tasks yang terkait kategori ini
                firestore.collection("tasks")
                    .whereEqualTo("category", categoryName)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot) {
                            // Perbarui setiap task menjadi kategori default
                            firestore.collection("tasks").document(document.id)
                                .update("category", "Tidak dikategorikan")
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Related tasks updated.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to update tasks.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to fetch related tasks.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete category.", Toast.LENGTH_SHORT).show()
            }
    }
}