package com.example.quizapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DifficultyActivity : AppCompatActivity() {

    private var currentCategory: String = "GK"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty)

        // CategoryActivity से भेजी गई कैटेगरी का नाम प्राप्त करना
        currentCategory = intent.getStringExtra("CATEGORY") ?: "GK"

        val btnEasy = findViewById<Button>(R.id.btnEasy)
        val btnMedium = findViewById<Button>(R.id.btnMedium)
        val btnHard = findViewById<Button>(R.id.btnHard)

        btnEasy.setOnClickListener {
            startQuiz("easy")
        }

        btnMedium.setOnClickListener {
            startQuiz("medium")
        }

        btnHard.setOnClickListener {
            startQuiz("hard")
        }
    }

    private fun startQuiz(difficulty: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("CATEGORY", currentCategory)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
    }
}