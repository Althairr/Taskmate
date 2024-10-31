package com.example.taskmate.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmate.R
import com.google.firebase.auth.FirebaseAuth
import com.example.taskmate.databinding.ActivityForgetPassBinding

class ForgetPassActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var binding : ActivityForgetPassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPassBinding.inflate(layoutInflater)
        setContentView(binding.root) // Menggunakan binding untuk mengatur layout

        auth = FirebaseAuth.getInstance()

        // Menggunakan binding untuk mengakses EditText dan Button
        binding.resetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Tolong masukkan emailnya", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(email)
            }
        }

        // Menggunakan binding untuk tvToLogin
        binding.tvToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Optional: menutup activity ini jika tidak ingin kembali ke sini
        }
    }

    private fun resetPassword(email: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        // Email tidak terdaftar
                        Toast.makeText(this, "Email tidak terdaftar", Toast.LENGTH_SHORT).show()
                    } else {
                        // Email terdaftar, kirim email reset kata sandi
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { resetTask ->
                                if (resetTask.isSuccessful) {
                                    Toast.makeText(this, "Email reset kata sandi telah dikirim", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Gagal mengirim email reset kata sandi", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    // Gagal memeriksa email
                    Toast.makeText(this, "Terjadi kesalahan. Coba lagi nanti.", Toast.LENGTH_SHORT).show()
                }
            }
    }

}