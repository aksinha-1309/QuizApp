package com.ankurapps.quizapp

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener


class ResultActivity : AppCompatActivity() {

    private var winMediaPlayer: MediaPlayer? = null
    private var earnedCoins = 0
    private var isClaimedDouble = false

    private lateinit var tvEarnedCoins: TextView
    private lateinit var btnDoubleCoins: Button
    private lateinit var bannerAdContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // --- Start.io SDK Initialization ---
        StartAppSDK.init(this, "209947092", false)

        val tvFinalScore = findViewById<TextView>(R.id.tvScore)
        val btnRestart = findViewById<Button>(R.id.btnPlayAgain)
        val lottieCelebration = findViewById<LottieAnimationView>(R.id.lottieCelebration)

        tvEarnedCoins = findViewById(R.id.tvEarnedCoins)
        btnDoubleCoins = findViewById(R.id.btnDoubleCoins)
        bannerAdContainer = findViewById(R.id.bannerAdContainer)

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 10)
        earnedCoins = intent.getIntExtra("EARNED_COINS", 0)

        tvFinalScore.text = "Your Score: $score / $total"
        tvEarnedCoins.text = "Coins Earned: +$earnedCoins 🪙"

        if (earnedCoins <= 0) {
            btnDoubleCoins.isEnabled = false
            btnDoubleCoins.alpha = 0.5f
        }

        loadBannerAd()

        winMediaPlayer = MediaPlayer.create(this, R.raw.win_sound)

        if (score >= 8) {
            lottieCelebration.visibility = View.VISIBLE
            lottieCelebration.repeatCount = 0
            lottieCelebration.playAnimation()

            winMediaPlayer?.start()

            winMediaPlayer?.setOnCompletionListener {
                winMediaPlayer?.release()
                winMediaPlayer = null
            }

            lottieCelebration.addAnimatorUpdateListener {
                if (lottieCelebration.progress >= 1.0f) {
                    lottieCelebration.visibility = View.GONE
                }
            }
        } else {
            lottieCelebration.visibility = View.GONE
        }

        btnDoubleCoins.setOnClickListener {
            if (!isClaimedDouble && earnedCoins > 0) {
                showRewardedAdForDoubleCoins()
            }
        }

        btnRestart.setOnClickListener {
            showInterstitialAdBeforeRestart()
        }
    }

    private fun loadBannerAd() {
        val banner = Banner(this)
        bannerAdContainer.addView(banner)
    }

    private fun showRewardedAdForDoubleCoins() {
        val rewardedAd = StartActivity.globalRewardedAd

        // 1. अगर बैकग्राउंड में लोड हुआ वीडियो ऐड रेडी है तो वही दिखाएं
        if (rewardedAd != null && rewardedAd.isReady) {
            rewardedAd.setVideoListener(object : VideoListener {
                override fun onVideoCompleted() {
                    doubleCoinsReward() // कॉइन्स 2X करें
                    Toast.makeText(this@ResultActivity, "🎉 Coins Doubled!", Toast.LENGTH_SHORT).show()
                    preloadNextAd()
                }
            })
            rewardedAd.showAd()
        } else {
            // 2. अगर वीडियो ऐड रेडी नहीं है, तो तुरंत Fullpage (Interstitial) ऐड चलाएं
            val fallbackAd = StartAppAd(this)
            fallbackAd.loadAd(StartAppAd.AdMode.FULLPAGE, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    fallbackAd.showAd()
                    doubleCoinsReward() // कॉइन्स 2X करें
                    Toast.makeText(this@ResultActivity, "🎉 Coins Doubled!", Toast.LENGTH_SHORT).show()
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    Toast.makeText(this@ResultActivity, "Ad loading, please try in 2s!", Toast.LENGTH_SHORT).show()
                }
            })
            preloadNextAd()
        }
    }

    private fun preloadNextAd() {
        StartActivity.globalRewardedAd = StartAppAd(this)
        StartActivity.globalRewardedAd?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO)
    }

    private fun showInterstitialAdBeforeRestart() {
        val interstitialAd = StartAppAd(this)
        interstitialAd.loadAd(StartAppAd.AdMode.FULLPAGE, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                interstitialAd.showAd(object : AdDisplayListener {
                    override fun adHidden(ad: Ad?) {
                        goToStartActivity()
                    }
                    override fun adDisplayed(ad: Ad?) {}
                    override fun adClicked(ad: Ad?) {}
                    override fun adNotDisplayed(ad: Ad?) {
                        goToStartActivity()
                    }
                })
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                goToStartActivity()
            }
        })
    }

    private fun goToStartActivity() {
        val intent = Intent(this, StartActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun doubleCoinsReward() {
        if (!isClaimedDouble) {
            addCoins(earnedCoins)
            isClaimedDouble = true

            val newTotalEarned = earnedCoins * 2
            tvEarnedCoins.text = "Coins Earned: +$newTotalEarned 🪙 (2X Claimed!)"

            btnDoubleCoins.isEnabled = false
            btnDoubleCoins.alpha = 0.5f
            btnDoubleCoins.text = "2X Claimed! ✅"

            Toast.makeText(this, "🎉 Reward Claimed! Earned Double Coins!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCoins(amount: Int) {
        val prefs = getSharedPreferences("QuizAppPrefs", MODE_PRIVATE)
        val currentCoins = prefs.getInt("total_coins", 0)
        val newTotal = currentCoins + amount
        prefs.edit().putInt("total_coins", newTotal).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        winMediaPlayer?.release()
    }
}