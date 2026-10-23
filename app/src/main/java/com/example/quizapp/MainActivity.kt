package com.ankurapps.quizapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ankurapps.quizapp.Model.QuestionModel
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import org.json.JSONArray


class MainActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var tvOptionA: TextView
    private lateinit var tvOptionB: TextView
    private lateinit var tvOptionC: TextView
    private lateinit var tvOptionD: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnLanguageToggle: Button
    private lateinit var tvCoinCounter: TextView
    private lateinit var btnLifeline5050: Button
    private lateinit var btnExtraTime: Button
    private lateinit var bannerAdContainer: FrameLayout

    private val questionList = ArrayList<QuestionModel>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var sessionEarnedCoins = 0

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 30000
    private var selectedCategory: String = "GK"
    private var selectedDifficulty: String = "easy"
    private var selectedLanguage: String = "EN"
    private var correctMediaPlayer: android.media.MediaPlayer? = null
    private var wrongMediaPlayer: android.media.MediaPlayer? = null

    private var is5050UsedInCurrentQuestion = false
    private var activeToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Start.io SDK Initialization ---
        StartAppSDK.init(this, "209947092", false)

        selectedCategory = intent.getStringExtra("CATEGORY") ?: "GK"
        selectedDifficulty = intent.getStringExtra("DIFFICULTY") ?: "easy"

        tvQuestion = findViewById(R.id.tvQuestion)
        tvOptionA = findViewById(R.id.tvOptionA)
        tvOptionB = findViewById(R.id.tvOptionB)
        tvOptionC = findViewById(R.id.tvOptionC)
        tvOptionD = findViewById(R.id.tvOptionD)
        tvTimer = findViewById(R.id.tvTimer)
        btnLanguageToggle = findViewById(R.id.btnLanguageToggle)
        tvCoinCounter = findViewById(R.id.tvCoinCounter)
        btnLifeline5050 = findViewById(R.id.btnLifeline5050)
        btnExtraTime = findViewById(R.id.btnExtraTime)
        bannerAdContainer = findViewById(R.id.bannerAdContainer)

        updateCoinDisplay()

        correctMediaPlayer = android.media.MediaPlayer.create(this, R.raw.correct)
        wrongMediaPlayer = android.media.MediaPlayer.create(this, R.raw.wrong)

        loadBannerAd()

        btnLanguageToggle.setOnClickListener {
            selectedLanguage = if (selectedLanguage == "EN") "HI" else "EN"
            val msg = if (selectedLanguage == "HI") "भाषा बदलकर हिंदी कर दी गई है 🇮🇳" else "Language changed to English 🇬🇧"
            showSafeToast(msg)
            displayQuestion()
        }

        btnLifeline5050.setOnClickListener {
            use5050Lifeline()
        }

        btnExtraTime.setOnClickListener {
            showRewardedAdForExtraTime()
        }

        loadQuestionsFromJSON()
    }

    // --- Start.io Banner Ad Setup ---
    private fun loadBannerAd() {
        val banner = Banner(this)
        bannerAdContainer.addView(banner)
    }

    // --- Start.io Rewarded Video Ad for Extra Time ---
    private fun showRewardedAdForExtraTime() {
        val rewardedAd = StartActivity.globalRewardedAd

        if (rewardedAd != null && rewardedAd.isReady) {
            rewardedAd.setVideoListener(object : VideoListener {
                override fun onVideoCompleted() {
                    grantExtraTime()
                    showSafeToast(if (selectedLanguage == "HI") "🎉 +15 सेकंड मिले!" else "🎉 Earned +15s Time!")
                    preloadNextAd()
                }
            })
            rewardedAd.showAd()
        } else {
            val fallbackAd = StartAppAd(this)
            fallbackAd.loadAd(StartAppAd.AdMode.FULLPAGE, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    fallbackAd.showAd()
                    grantExtraTime()
                    showSafeToast(if (selectedLanguage == "HI") "🎉 +15 सेकंड मिले!" else "🎉 Earned +15s Time!")
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    showSafeToast(if (selectedLanguage == "HI") "ऐड लोड हो रहा है, कृपया 2 सेकंड बाद दबाएं" else "Ad loading, try again in 2s")
                }
            })
            preloadNextAd()
        }
    }

    private fun preloadNextAd() {
        StartActivity.globalRewardedAd = StartAppAd(this)
        StartActivity.globalRewardedAd?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO)
    }

    private fun grantExtraTime() {
        timeLeftInMillis += 15000
        startTimer(timeLeftInMillis)
    }

    private fun showSafeToast(message: String) {
        activeToast?.cancel()
        activeToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        activeToast?.show()
    }

    private fun getCorrectQuestions(): MutableSet<String> {
        val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
        return prefs.getStringSet("correct_questions_${selectedCategory}_$selectedDifficulty", mutableSetOf()) ?: mutableSetOf()
    }

    private fun markQuestionAsCorrect(questionText: String) {
        val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
        val correctSet = getCorrectQuestions().toMutableSet()
        correctSet.add(questionText)
        prefs.edit().putStringSet("correct_questions_${selectedCategory}_$selectedDifficulty", correctSet).apply()
    }

    private fun clearCorrectQuestions() {
        val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
        prefs.edit().remove("correct_questions_${selectedCategory}_$selectedDifficulty").apply()
    }

    private fun getTotalCoins(): Int {
        val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
        return prefs.getInt("total_coins", 0)
    }

    private fun addCoins(amount: Int) {
        val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
        val currentCoins = getTotalCoins()
        val newTotal = currentCoins + amount
        prefs.edit().putInt("total_coins", newTotal).apply()
    }

    private fun deductCoins(amount: Int): Boolean {
        val currentCoins = getTotalCoins()
        if (currentCoins >= amount) {
            val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
            prefs.edit().putInt("total_coins", currentCoins - amount).apply()
            updateCoinDisplay()
            return true
        }
        return false
    }

    private fun updateCoinDisplay() {
        tvCoinCounter.text = "🪙 ${getTotalCoins()}"
    }

    private fun loadQuestionsFromJSON() {
        try {
            val jsonString = assets.open("questions.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            val rawList = ArrayList<QuestionModel>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val category = jsonObject.getString("category")
                val difficulty = jsonObject.optString("difficulty", "easy")

                // Strict Category and Difficulty Filtering
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
                    rawList.add(questionModel)
                }
            }

            if (rawList.isNotEmpty()) {
                // 1. JSON Deduplication (REMOVE duplicates inside JSON itself by unique question text)
                val uniqueJsonList = rawList.distinctBy { it.questionEn.trim().lowercase() }.toMutableList()

                val correctSet = getCorrectQuestions()

                // 2. Filter out already correctly answered questions in this category/difficulty
                var filteredList = uniqueJsonList.filter {
                    !correctSet.contains(it.questionEn)
                }.toMutableList()

                // If all questions are finished, reset cycle cleanly
                if (filteredList.isEmpty()) {
                    val resetMessage = if (selectedLanguage == "HI") {
                        "बधाई हो! 🎉 आपने इस सेक्शन के सभी सवाल पूरे कर लिए हैं। नया साइकल शुरू हो रहा है!"
                    } else {
                        "Congratulations! 🎉 You have completed all questions in this section. Starting a new cycle!"
                    }

                    showSafeToast(resetMessage)
                    clearCorrectQuestions()
                    filteredList = uniqueJsonList.toMutableList()
                }

                // 3. Shuffle thoroughly for complete randomness per game session
                filteredList.shuffle()

                questionList.clear()
                val limit = if (filteredList.size > 10) 10 else filteredList.size
                for (i in 0 until limit) {
                    questionList.add(filteredList[i])
                }

                currentQuestionIndex = 0
                displayQuestion()
            } else {
                showSafeToast("इस डिफिकल्टी में कोई सवाल नहीं मिला!")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showSafeToast("JSON लोड एरर: ${e.message}")
        }
    }

    private fun displayQuestion() {
        resetOptionsColor()
        timeLeftInMillis = 30000
        startTimer(timeLeftInMillis)

        is5050UsedInCurrentQuestion = false
        btnLifeline5050.isEnabled = true
        btnLifeline5050.alpha = 1.0f

        val tvQuestionCounter = findViewById<TextView>(R.id.tvQuestionCounter)
        tvQuestionCounter.text = "Question: ${currentQuestionIndex + 1}/${questionList.size}"

        val currentModel = questionList[currentQuestionIndex]

        val qText = if (selectedLanguage == "HI") currentModel.questionHi else currentModel.questionEn
        tvQuestion.text = qText

        val optA = if (selectedLanguage == "HI") currentModel.optionAHi else currentModel.optionAEn
        val optB = if (selectedLanguage == "HI") currentModel.optionBHi else currentModel.optionBEn
        val optC = if (selectedLanguage == "HI") currentModel.optionCHi else currentModel.optionCEn
        val optD = if (selectedLanguage == "HI") currentModel.optionDHi else currentModel.optionDEn

        val correctAnswer = if (selectedLanguage == "HI") currentModel.correctAnswerHi else currentModel.correctAnswerEn

        val optionList = mutableListOf(
            Pair(optA, optA == correctAnswer),
            Pair(optB, optB == correctAnswer),
            Pair(optC, optC == correctAnswer),
            Pair(optD, optD == correctAnswer)
        )

        optionList.shuffle()

        tvOptionA.text = optionList[0].first
        tvOptionB.text = optionList[1].first
        tvOptionC.text = optionList[2].first
        tvOptionD.text = optionList[3].first

        tvOptionA.tag = optionList[0].second
        tvOptionB.tag = optionList[1].second
        tvOptionC.tag = optionList[2].second
        tvOptionD.tag = optionList[3].second

        // Re-enable options and set click listeners safely
        tvOptionA.isClickable = true
        tvOptionB.isClickable = true
        tvOptionC.isClickable = true
        tvOptionD.isClickable = true

        tvOptionA.setOnClickListener { checkAnswer(tvOptionA, optionList[0].second, currentModel) }
        tvOptionB.setOnClickListener { checkAnswer(tvOptionB, optionList[1].second, currentModel) }
        tvOptionC.setOnClickListener { checkAnswer(tvOptionC, optionList[2].second, currentModel) }
        tvOptionD.setOnClickListener { checkAnswer(tvOptionD, optionList[3].second, currentModel) }
    }

    private fun use5050Lifeline() {
        if (is5050UsedInCurrentQuestion) {
            val msg = if (selectedLanguage == "HI") "आप इस सवाल में 50-50 यूज़ कर चुके हैं!" else "Already used in this question!"
            showSafeToast(msg)
            return
        }

        if (deductCoins(20)) {
            is5050UsedInCurrentQuestion = true
            btnLifeline5050.isEnabled = false
            btnLifeline5050.alpha = 0.5f

            val optionViews = listOf(tvOptionA, tvOptionB, tvOptionC, tvOptionD)
            val wrongOptions = optionViews.filter { (it.tag as? Boolean) == false }

            val optionsToHide = wrongOptions.shuffled().take(2)
            for (optView in optionsToHide) {
                optView.visibility = View.INVISIBLE
                optView.isClickable = false
            }

            val msg = if (selectedLanguage == "HI") "50:50 लाइफलाइन यूज़ हो गई! (-20 🪙)" else "50:50 Lifeline Used! (-20 🪙)"
            showSafeToast(msg)
        } else {
            val msg = if (selectedLanguage == "HI") "कम से कम 20 कॉइंस चाहिए!" else "You need at least 20 Coins!"
            showSafeToast(msg)
        }
    }

    private fun startTimer(duration: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                tvTimer.text = "Time: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvTimer.text = "Time: 0s"
                disableClickOnOptions()

                showSafeToast("Time's up! ⏰")

                Handler(Looper.getMainLooper()).postDelayed({
                    currentQuestionIndex++
                    if (currentQuestionIndex < questionList.size) {
                        displayQuestion()
                    } else {
                        goToResultActivity()
                    }
                }, 1000)
            }
        }.start()
    }

    private fun checkAnswer(selectedTextView: TextView, isCorrect: Boolean, currentModel: QuestionModel) {
        countDownTimer?.cancel()
        disableClickOnOptions()

        val message: String

        if (isCorrect) {
            selectedTextView.setBackgroundColor(Color.parseColor("#A7DE83"))
            score++
            sessionEarnedCoins += 10
            correctMediaPlayer?.start()
            message = if (selectedLanguage == "HI") "सही उत्तर! 🎉 (+10 कॉइंस)" else "Correct Answer! 🎉 (+10 Coins)"

            markQuestionAsCorrect(currentModel.questionEn)
            addCoins(10)
            updateCoinDisplay()
        } else {
            selectedTextView.setBackgroundColor(Color.parseColor("#df5d65"))
            wrongMediaPlayer?.start()
            message = if (selectedLanguage == "HI") "गलत उत्तर! ❌" else "Wrong Answer! ❌"
        }

        showSafeToast(message)

        Handler(Looper.getMainLooper()).postDelayed({
            currentQuestionIndex++
            if (currentQuestionIndex < questionList.size) {
                displayQuestion()
            } else {
                goToResultActivity()
            }
        }, 1000)
    }

    private fun goToResultActivity() {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL", questionList.size)
        intent.putExtra("EARNED_COINS", sessionEarnedCoins)
        startActivity(intent)
        finish()
    }

    private fun resetOptionsColor() {
        val defaultColor = Color.parseColor("#E0E0E0")
        val optionViews = listOf(tvOptionA,tvOptionB, tvOptionC, tvOptionD)

        for (optView in optionViews) {
            optView.setBackgroundColor(defaultColor)
            optView.visibility = View.VISIBLE
            optView.isClickable = true
        }
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