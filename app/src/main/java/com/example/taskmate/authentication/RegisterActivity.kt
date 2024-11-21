package com.example.taskmate.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmate.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding
    private val firestore = FirebaseFirestore.getInstance() // Initialize Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        // If the button clicked, go to the login
        binding.tvToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.edtRegisterEmail.text.toString()
            val password = binding.edtRegisterPassword.text.toString()
            val username = binding.edtRegisterUsername.text.toString()

            // Validate input fields
            if (email.isEmpty()) {
                binding.edtRegisterEmail.error = "Email harus diisi"
                binding.edtRegisterEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.edtRegisterEmail.error = "Email tidak valid"
                binding.edtRegisterEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.edtRegisterPassword.error = "Password harus diisi"
                binding.edtRegisterPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.edtRegisterPassword.error = "Panjang password minimal 6 karakter"
                binding.edtRegisterPassword.requestFocus()
                return@setOnClickListener
            }

            if (username.isEmpty()) {
                binding.edtRegisterUsername.error = "Nama harus diisi"
                binding.edtRegisterUsername.requestFocus()
                return@setOnClickListener
            }

            if (username.any { it.isDigit() }) {
                binding.edtRegisterUsername.error = "Nama tidak boleh ada angka"
                binding.edtRegisterUsername.requestFocus()
                return@setOnClickListener
            }

            // Handle Firebase register
            registerFirebase(email, username, password)
        }
    }

    private fun registerFirebase(email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Get the current user ID
                    val userId = auth.currentUser?.uid

                    if (userId == null) {
                        Toast.makeText(this, "Error: User ID is null", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // Prepare the user data
                    val user = hashMapOf(
                        "userId" to userId,
                        "username" to username,
                        "email" to email
                    )

                    // Save user data to Firestore
                    firestore.collection("users").document(userId).set(user)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "Berhasil daftar!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Gagal menyimpan data pengguna: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}