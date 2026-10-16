package com.example.quizapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val btnStartQuiz = findViewById<Button>(R.id.btnStartQuiz)

        // "Start Quiz" बटन दबाने पर MainActivity (Quiz Page) खुलेगा
        btnStartQuiz.setOnClickListener {
            val intent = Intent(this, categoryActivity::class.java)
            startActivity(intent)
        }
    }
}