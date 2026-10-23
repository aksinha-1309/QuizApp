package com.ankurapps.quizapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class categoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        val btnGK = findViewById<AppCompatButton>(R.id.btnCategoryGK)
        val btnAptitude = findViewById<AppCompatButton>(R.id.btnCategoryAptitude)
        val btnCoding = findViewById<AppCompatButton>(R.id.btnCategoryCoding)
        val btnEnglish = findViewById<AppCompatButton>(R.id.btnCategoryEnglish)

        btnGK.setOnClickListener {
            openDifficultyScreen("GK")
        }

        btnAptitude.setOnClickListener {
            openDifficultyScreen("Aptitude")
        }

        btnCoding.setOnClickListener {
            openDifficultyScreen("Coding")
        }

        btnEnglish.setOnClickListener {
            openDifficultyScreen("English")
        }
    }

    private fun openDifficultyScreen(categoryName: String) {
        val intent = Intent(this, DifficultyActivity::class.java)
        intent.putExtra("CATEGORY", categoryName)
        startActivity(intent)
    }
}