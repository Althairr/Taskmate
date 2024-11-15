package com.example.taskmate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.taskmate.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import com.example.taskmate.authentication.LoginActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("users")
    private val firestore = FirebaseFirestore.getInstance() // Initialize Firestor

    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root // Ensure this returns the inflated view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the button by its ID
        val btnHapusAkun = view.findViewById<Button>(R.id.btn_hapus_akun)
        val btnKeluar = view.findViewById<Button>(R.id.btn_keluar)

        // Set click listeners for both the ImageView and ImageButton
        binding.profileImage.setOnClickListener {
            checkStoragePermissionAndOpenGallery()
        }
        binding.profileAdd.setOnClickListener {
            checkStoragePermissionAndOpenGallery()
        }

        displayUserData()

        binding.btnSave.setOnClickListener {
            val nickname = binding.editNama.text.toString().trim()
            if (nickname.isEmpty()) {
                Toast.makeText(requireContext(), "Nama harus diisi.", Toast.LENGTH_SHORT).show()
            } else {
                updateUserData(nickname = nickname)
            }
        }

        btnKeluar.setOnClickListener {
            logoutUser()
        }

        btnHapusAkun.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        // Create and show an AlertDialog to confirm account deletion
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus Akun")
            .setMessage("Apakah Anda yakin ingin menghapus akun ini? Tindakan ini tidak dapat diurungkan.")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()
                deleteUserAccount()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissions(arrayOf(permission), REQUEST_STORAGE_PERMISSION)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            binding.profileImage.setImageURI(imageUri) // Show selected image
            uploadProfileImage()
        }
    }

    /// Save profile image URI to Firestore
    private fun uploadProfileImage() {
        imageUri?.let { uri ->
            updateUserData(profileImageUrl = uri.toString())
        }
    }

    // Combined function to update nickname or profile image URL
    private fun updateUserData(nickname: String? = null, profileImageUrl: String? = null) {
        val userId = auth.currentUser?.uid ?: return

        // Create a map of the data to be saved
        val data = mutableMapOf<String, Any>()
        nickname?.let { data["nickname"] = it }
        profileImageUrl?.let { data["profileImageUrl"] = it }

        // Use SetOptions.merge() to ensure data is merged rather than overwritten
        firestore.collection("users").document(userId).set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data pengguna diperbarui.", Toast.LENGTH_SHORT).show()
                nickname?.let {
                    binding.profileName.text = it
                    binding.editNama.setText(it)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memperbarui data pengguna.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            binding.profileEmail.text = "Email tidak tersedia"
            binding.editTextEmail.setText("Email tidak tersedia")
            return
        }

        // Load email
        binding.profileEmail.text = auth.currentUser?.email ?: "Email tidak tersedia"
        binding.editTextEmail.setText(auth.currentUser?.email ?: "Email tidak tersedia")

        // Load user data from Firestore
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("username")
                    val profileImageUrl = document.getString("profileImageUrl")

                    // Set nickname and profile image
                    binding.profileName.text = nickname ?: "Nama tidak tersedia"
                    binding.editNama.setText(nickname ?: "Nama tidak tersedia")
                    profileImageUrl?.let { loadProfileImage(it) }
                } else {
                    binding.profileName.text = "Nama tidak tersedia"
                    binding.editNama.setText("Nama tidak tersedia")
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat data pengguna.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.fikri) // Optional placeholder
            .into(binding.profileImage)
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        val userId = user?.uid ?: return

        // Step 1: Delete user data from Firestore
        firestore.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                // Step 2: Delete user from Firebase Authentication
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Akun berhasil dihapus.", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal menghapus akun dari Authentication.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal menghapus data dari Firestore.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 1
    }

}