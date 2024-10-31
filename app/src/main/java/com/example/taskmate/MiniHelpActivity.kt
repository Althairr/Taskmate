package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.taskmate.R
import com.example.taskmate.authentication.LoginActivity

class MiniHelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mini_help)
        val btnCont = findViewById<Button>(R.id.button_continue)

        btnCont.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }
}