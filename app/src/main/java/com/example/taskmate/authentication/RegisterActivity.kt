package com.example.taskmate.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmate.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    lateinit private var auth : FirebaseAuth
    lateinit private var binding : ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
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

        binding.btnRegister.setOnClickListener{
            val email = binding.edtRegisterEmail.text.toString()
            val password = binding.edtRegisterPassword.text.toString()
            val username = binding.edtRegisterUsername.text.toString()

            // Ensure that the user has email
            if (email.isEmpty())
            {
                binding.edtRegisterEmail.error = "Email harus diisi"
                binding.edtRegisterEmail.requestFocus()
                return@setOnClickListener
            }

            // Validate if email matches or not
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            {
                binding.edtRegisterPassword.error = "Email tidak valid"
                binding.edtRegisterEmail.requestFocus()
                return@setOnClickListener
            }

            // Ensure that the user has password
            if (password.isEmpty())
            {
                binding.edtRegisterPassword.error = "Password harus diisi"
                binding.edtRegisterPassword.requestFocus()
                return@setOnClickListener
            }

            // Ensure that the password is valid
            if (password.length < 6)
            {
                binding.edtRegisterPassword.error = "Panjang password minimal 6 karakter"
                binding.edtRegisterPassword.requestFocus()
                return@setOnClickListener
            }

            // Handle the username
            if (username.isEmpty())
            {
                binding.edtRegisterEmail.error = "Nama harus diisi"
                binding.edtRegisterEmail.requestFocus()
                return@setOnClickListener
            }

            // Ensure that the username only consists of alphabet
            if (username.any { it.isDigit() }) {
                binding.edtRegisterEmail.error = "Nama tidak boleh ada angka"
                binding.edtRegisterEmail.requestFocus()
                return@setOnClickListener
            }


            // Handle the firebase register
            registerFirebase(email, username, password)
        }
    }

    private fun registerFirebase(email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Get the current user id
                    val userId = auth.currentUser?.uid

                    // Prepare
                    val user = User(userId, username, email) // Asumsikan Anda memiliki kelas User

                    // Save to database
                    val database = FirebaseDatabase.getInstance().getReference("users")
                    userId?.let {
                        database.child(it).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Berhasil daftar!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "Gagal menyimpan data pengguna: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

}