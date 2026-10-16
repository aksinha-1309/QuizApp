package com.example.quizapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quizapp.Model.QuestionModel
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var tvOptionA: TextView
    private lateinit var tvOptionB: TextView
    private lateinit var tvOptionC: TextView
    private lateinit var tvOptionD: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnLanguageToggle: Button

    private val questionList = ArrayList<QuestionModel>()
    private var currentQuestionIndex = 0
    private var score = 0

    private var countDownTimer: CountDownTimer? = null
    private var selectedCategory: String = "GK"
    private var selectedDifficulty: String = "easy"
    private var selectedLanguage: String = "EN" // डिफ़ॉल्ट इंग्लिश
    private var correctMediaPlayer: android.media.MediaPlayer? = null
    private var wrongMediaPlayer: android.media.MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Intent से Category और Difficulty प्राप्त करना
        selectedCategory = intent.getStringExtra("CATEGORY") ?: "GK"
        selectedDifficulty = intent.getStringExtra("DIFFICULTY") ?: "easy"

        // Views initialize करना
        tvQuestion = findViewById(R.id.tvQuestion)
        tvOptionA = findViewById(R.id.tvOptionA)
        tvOptionB = findViewById(R.id.tvOptionB)
        tvOptionC = findViewById(R.id.tvOptionC)
        tvOptionD = findViewById(R.id.tvOptionD)
        tvTimer = findViewById(R.id.tvTimer)
        btnLanguageToggle = findViewById(R.id.btnLanguageToggle)

        correctMediaPlayer = android.media.MediaPlayer.create(this, R.raw.correct)
        wrongMediaPlayer = android.media.MediaPlayer.create(this, R.raw.wrong)

        // भाषा बदलने वाले बटन का क्लिक लिसनर
        btnLanguageToggle.setOnClickListener {
            val message: String
            if (selectedLanguage == "EN") {
                selectedLanguage = "HI"
                message = "भाषा बदलकर हिंदी कर दी गई है 🇮🇳"
            } else {
                selectedLanguage = "EN"
               message="Language changed to English 🇬🇧"
            }
            val customToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            customToast.show()

            Handler(Looper.getMainLooper()).postDelayed({
                customToast.cancel()
            }, 1000)
            // भाषा बदलते ही तुरंत current question को दोबारा रीफ्रेश/डिस्प्ले करो
            displayQuestion()
        }

        loadQuestionsFromJSON()
    }

    private fun loadQuestionsFromJSON() {
        try {
            val jsonString = assets.open("questions.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            questionList.clear()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val category = jsonObject.getString("category")
                val difficulty = jsonObject.optString("difficulty", "easy")

                // Category और Difficulty दोनों मैच होनी चाहिए
                if (category.equals(selectedCategory, ignoreCase = true) &&
                    difficulty.equals(selectedDifficulty, ignoreCase = true)) {

                    val questionModel = QuestionModel(
                        category = category,
                        difficulty = difficulty,
                        questionEn = jsonObject.optString("questionEn"),
                        questionHi = jsonObject.optString("questionHi"),
                        optionAEn = jsonObject.optString("optionAEn"),
                        optionAHi = jsonObject.optString("optionAHi"),
                        optionBEn = jsonObject.optString("optionBEn"),
                        optionBHi = jsonObject.optString("optionBHi"),
                        optionCEn = jsonObject.optString("optionCEn"),
                        optionCHi = jsonObject.optString("optionCHi"),
                        optionDEn = jsonObject.optString("optionDEn"),
                        optionDHi = jsonObject.optString("optionDHi"),
                        correctAnswerEn = jsonObject.optString("correctAnswerEn"),
                        correctAnswerHi = jsonObject.optString("correctAnswerHi")
                    )
                    questionList.add(questionModel)
                }
            }

            if (questionList.isNotEmpty()) {
                // यहाँ सारे सवाल आपस में शफल हो जाएंगे ताकि बार-बार एक ही क्रम में न आएं
                questionList.shuffle()

                // 10 सवालों की लिमिट सेट करना
                val limitedList = ArrayList<QuestionModel>()
                val limit = if (questionList.size > 10) 10 else questionList.size
                for (i in 0 until limit) {
                    limitedList.add(questionList[i])
                }

                questionList.clear()
                questionList.addAll(limitedList)

                displayQuestion()
            } else {
                Toast.makeText(this, "इस डिफिकल्टी में कोई सवाल नहीं मिला!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "JSON लोड एरर: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayQuestion() {
        resetOptionsColor()
        startTimer()

        val tvQuestionCounter = findViewById<TextView>(R.id.tvQuestionCounter)
        tvQuestionCounter.text = "Question: ${currentQuestionIndex + 1}/${questionList.size}"

        val currentModel = questionList[currentQuestionIndex]

        // भाषा के हिसाब से सवाल और ऑप्शंस सेट करना
        val qText = if (selectedLanguage == "HI") currentModel.questionHi else currentModel.questionEn
        tvQuestion.text = qText

        val optA = if (selectedLanguage == "HI") currentModel.optionAHi else currentModel.optionAEn
        val optB = if (selectedLanguage == "HI") currentModel.optionBHi else currentModel.optionBEn
        val optC = if (selectedLanguage == "HI") currentModel.optionCHi else currentModel.optionCEn
        val optD = if (selectedLanguage == "HI") currentModel.optionDHi else currentModel.optionDEn

        val correctAnswer = if (selectedLanguage == "HI") currentModel.correctAnswerHi else currentModel.correctAnswerEn

        // ऑप्शंस और उनके सही होने की स्थिति को लिस्ट में रखकर शफल करना
        val optionList = mutableListOf(
            Pair(optA, optA == correctAnswer),
            Pair(optB, optB == correctAnswer),
            Pair(optC, optC == correctAnswer),
            Pair(optD, optD == correctAnswer)
        )

        // यहाँ ऑप्शंस की पोजीशन रैंडम हो जाएगी
        optionList.shuffle()

        // शफल होने के बाद TextView में टेक्स्ट सेट करना
        tvOptionA.text = optionList[0].first
        tvOptionB.text = optionList[1].first
        tvOptionC.text = optionList[2].first
        tvOptionD.text = optionList[3].first

        // क्लिक करने पर चेक करना कि वह ऑप्शन सही है या नहीं
        tvOptionA.setOnClickListener { checkAnswer(tvOptionA, optionList[0].second) }
        tvOptionB.setOnClickListener { checkAnswer(tvOptionB, optionList[1].second) }
        tvOptionC.setOnClickListener { checkAnswer(tvOptionC, optionList[2].second) }
        tvOptionD.setOnClickListener { checkAnswer(tvOptionD, optionList[3].second) }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "Time: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvTimer.text = "Time: 0s"
                disableClickOnOptions()

                val timeUpToast = Toast.makeText(this@MainActivity, "Time's up! ⏰", Toast.LENGTH_SHORT)
                timeUpToast.show()

                Handler(Looper.getMainLooper()).postDelayed({
                    timeUpToast.cancel()
                }, 1000)

                Handler(Looper.getMainLooper()).postDelayed({
                    currentQuestionIndex++
                    if (currentQuestionIndex < questionList.size) {
                        displayQuestion()
                    } else {
                        goToResultActivity()
                    }
                }, 1500)
            }
        }.start()
    }

    private fun checkAnswer(selectedTextView: TextView, isCorrect: Boolean) {
        countDownTimer?.cancel()
        disableClickOnOptions()

        val message: String

        if (isCorrect) {
            selectedTextView.setBackgroundColor(Color.parseColor("#A7DE83"))
            score++
            correctMediaPlayer?.start()
            message = "Correct Answer! 🎉"
        } else {
            selectedTextView.setBackgroundColor(Color.parseColor("#df5d65"))
            wrongMediaPlayer?.start()
            message = "Wrong Answer!❌"
        }

        val customToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        customToast.show()

        Handler(Looper.getMainLooper()).postDelayed({
            customToast.cancel()
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            currentQuestionIndex++
            if (currentQuestionIndex < questionList.size) {
                displayQuestion()
            } else {
                goToResultActivity()
            }
        }, 1500)
    }

    private fun goToResultActivity() {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL", questionList.size)
        startActivity(intent)
        finish()
    }

    private fun resetOptionsColor() {
        val defaultColor = Color.parseColor("#E0E0E0")
        tvOptionA.setBackgroundColor(defaultColor)
        tvOptionB.setBackgroundColor(defaultColor)
        tvOptionC.setBackgroundColor(defaultColor)
        tvOptionD.setBackgroundColor(defaultColor)

        tvOptionA.isClickable = true
        tvOptionB.isClickable = true
        tvOptionC.isClickable = true
        tvOptionD.isClickable = true
    }

    private fun disableClickOnOptions() {
        tvOptionA.isClickable = false
        tvOptionB.isClickable = false
        tvOptionC.isClickable = false
        tvOptionD.isClickable = false
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        correctMediaPlayer?.release()
        wrongMediaPlayer?.release()
    }
}