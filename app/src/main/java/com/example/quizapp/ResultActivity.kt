package com.example.quizapp

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class ResultActivity : AppCompatActivity() {

    private var winMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 2. व्यूज़ को इनिशियलाइज करें
        val tvFinalScore = findViewById<TextView>(R.id.tvScore)
        val btnRestart = findViewById<Button>(R.id.btnPlayAgain)
        val lottieCelebration = findViewById<LottieAnimationView>(R.id.lottieCelebration)

        // 3. स्कोर प्राप्त करें और दिखाएं
        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 10)
        tvFinalScore.text = "Your Score: $score / $total"

        // 4. विनिंग साउंड तैयार करें (सुनिश्चित करें कि res/raw/win_sound.mp3 मौजूद है)
        winMediaPlayer = MediaPlayer.create(this, R.raw.win_sound)

        // 5. अगर स्कोर 8 या उससे ज़्यादा है, तो Lottie एनिमेशन और साउंड चलाएं
        if (score >= 8) {
            lottieCelebration.visibility = View.VISIBLE
            lottieCelebration.playAnimation()
            winMediaPlayer?.start()
        } else {
            lottieCelebration.visibility = View.GONE
        }

        // 7. प्ले अगेन बटन का क्लिक लिसनर (बिना किसी ऐड के सीधा MainActivity पर जाएगा)
        btnRestart.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        winMediaPlayer?.release()
    }
}