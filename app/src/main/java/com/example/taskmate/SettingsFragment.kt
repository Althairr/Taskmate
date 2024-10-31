package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.example.taskmate.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.taskmate.authentication.LoginActivity

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("users")

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
        val btnGantiProfile = view.findViewById<ImageButton>(R.id.profile_add)
        val btnKeluar = view.findViewById<Button>(R.id.btn_keluar)


        // Call the function
        displayUserEmail()
        displayUserName()

        binding.btnSave.setOnClickListener {
            val nickname = binding.editNama.text.toString().trim()

            if (nickname.isEmpty()) {
                Toast.makeText(requireContext(), "Nama harus diisi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateUserNickname(nickname)
        }

        // Set click listener for the button
        btnHapusAkun.setOnClickListener {
            // Show a toast message
            Toast.makeText(requireContext(), "Fitur belum diimplementasikan", Toast.LENGTH_SHORT).show()
        }

        btnGantiProfile.setOnClickListener {
            // Show a toast message
            Toast.makeText(requireContext(), "Fitur belum diimplementasikan", Toast.LENGTH_SHORT).show()
        }

        btnKeluar.setOnClickListener {
            logoutUser()
        }
    }

    private fun updateUserNickname(nickname : String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "nickname" to nickname
        )

        // Update only the nickname in Firebase
        database.child(userId).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Nama berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            binding.profileName.text = nickname
            binding.editNama.setText(nickname)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memperbarui nama.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayUserEmail() {
        // Get the current user's email
        val userEmail = auth.currentUser?.email

        // Display the email in the TextView
        binding.profileEmail.text = userEmail ?: "Email tidak tersedia"
        binding.editTextEmail.setText(userEmail ?: "Email tidak tersedia")
    }

    private fun displayUserName() {
        // Get the current user's ID
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // User is not logged in, so display an error message
            binding.profileEmail.text = "Email tidak tersedia"
            binding.editTextEmail.setText("Email tidak tersedia")
            return
        }

        // Retrieve the username from the Firebase Realtime Database
        database.child(userId).child("username").get().addOnSuccessListener { snapshot ->
            val username = snapshot.value as? String

            // Display the username in the TextViews
            binding.profileName.text = username ?: "Nama tidak tersedia"
            binding.editNama.setText(username ?: "Nama tidak tersedia")
        }.addOnFailureListener {
            // In case of failure, show a default message
            binding.profileName.text = "Gagal memuat nama"
            binding.editNama.setText("Gagal memuat nama")
        }
    }


    private fun logoutUser() {
        // Log out the current user
        auth.signOut()

        // Redirect to the login activity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

}