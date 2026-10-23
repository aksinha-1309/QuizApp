package com.ankurapps.quizapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

class StartActivity : AppCompatActivity() {

    private lateinit var btnStartQuiz: Button

    companion object {
        // पूरी ऐप में कहीं भी इस्तेमाल करने के लिए ग्लोबल रिवार्ड ऐड वेरिएबल
        var globalRewardedAd: StartAppAd? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // --- Start.io SDK Initialization ---
        StartAppSDK.init(this, "209947092", false) // true = Test Mode

        btnStartQuiz = findViewById(R.id.btnStartQuiz)

        // ऐप शुरू होते ही बैकग्राउंड में रिवार्ड ऐड लोड करना शुरू कर दें
        preloadGlobalRewardedAd()

        btnStartQuiz.setOnClickListener {
            // डबल क्लिक या बटन फ्रीज होने से बचाने के लिए तुरंत डिसेबल करके फिर इनेबल करेंगे
            btnStartQuiz.isEnabled = false

            val intent = Intent(this, categoryActivity::class.java)
            startActivity(intent)
        }
    }

    // सबसे जरूरी: जब भी यूजर रिजल्ट या दूसरे पेज से वापस इस स्क्रीन पर लौटेगा, यह चलेगा
    override fun onResume() {
        super.onResume()
        // पक्का करेंगे कि स्टार्ट बटन हमेशा क्लिक करने के लिए तैयार (Enabled) मिले
        if (::btnStartQuiz.isInitialized) {
            btnStartQuiz.isEnabled = true
            btnStartQuiz.alpha = 1.0f
        }

        // अगर पिछला ऐड खत्म हो गया है, तो नया प्रीलोड कर लें
        if (globalRewardedAd == null || !globalRewardedAd!!.isReady) {
            preloadGlobalRewardedAd()
        }
    }

    private fun preloadGlobalRewardedAd() {
        globalRewardedAd = StartAppAd(this)
        globalRewardedAd?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                // ऐड सफलतापूर्वक लोड हो चुका है!
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                globalRewardedAd = null
            }
        })
    }
}