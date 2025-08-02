package com.example.todochiapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoSplash)
        val appName = findViewById<TextView>(R.id.tvSplashName)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)

        logo.startAnimation(zoomIn)
        appName.startAnimation(fadeIn)

        val lottieView = findViewById<LottieAnimationView>(R.id.lottieLoading)
        lottieView.setAnimation("loading_animation.json")
        lottieView.playAnimation()

        // Delay sebelum lanjut ke LoginActivity
        Handler(mainLooper).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 3000)     // bisa kamu sesuaikan waktunya
    }
}
