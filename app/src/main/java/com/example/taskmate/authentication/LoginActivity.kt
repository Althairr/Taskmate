package com.example.taskmate.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmate.MainActivity
import com.example.taskmate.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth
    private lateinit var binding : ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.tvToRegister.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.tvToForgotPassword.setOnClickListener{
            val intent = Intent(this, ForgetPassActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener{
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()

            // Ensure that the user has email
            if (email.isEmpty())
            {
                binding.edtEmail.error = "Email harus diisi"
                binding.edtEmail.requestFocus()
                return@setOnClickListener
            }

            // Validate if email matches or not
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            {
                binding.edtPassword.error = "Email tidak valid"
                binding.edtEmail.requestFocus()
                return@setOnClickListener
            }

            // Ensure that the user has password
            if (password.isEmpty())
            {
                binding.edtPassword.error = "Password harus diisi"
                binding.edtPassword.requestFocus()
                return@setOnClickListener
            }

            // Ensure that the password is valid
            if (password.length < 6)
            {
                binding.edtPassword.error = "Panjang password minimal 6 karakter"
                binding.edtPassword.requestFocus()
                return@setOnClickListener
            }

            // Handle the firebase login
            loginFirebase(email, password)
        }
    }

    private fun loginFirebase(email : String, password : String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if (it.isSuccessful)
                {
                    Toast.makeText(this, "Berhasil Login!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }
}