package com.example.todochiapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var lastFragmentIndex = -1

    companion object {
        const val INDEX_DASHBOARD = 0
        const val INDEX_TASK = 1
        const val INDEX_CALENDAR = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek login
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottomNavigation)

        // Default fragment saat pertama buka
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment(), INDEX_DASHBOARD)
            bottomNav.selectedItemId = R.id.nav_dashboard
        }

        // Bottom Nav item click
        bottomNav.setOnItemSelectedListener { item ->
            val (fragment, index) = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment() to INDEX_DASHBOARD
                R.id.nav_task -> TaskFragment() to INDEX_TASK
                R.id.nav_calendar -> CalendarFragment() to INDEX_CALENDAR
                else -> return@setOnItemSelectedListener false
            }

            // Trigger bounce animasi icon
            val itemView = bottomNav.findViewById<View>(item.itemId)
            animateBounce(itemView)

            if (index != lastFragmentIndex) {
                loadFragment(fragment, index)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment, newIndex: Int) {
        val enterAnim = if (newIndex > lastFragmentIndex) R.anim.slide_in_right else R.anim.slide_in_left
        val exitAnim = if (newIndex > lastFragmentIndex) R.anim.slide_out_left else R.anim.slide_out_right

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim)
            .replace(R.id.fragment_container, fragment)
            .commit()

        lastFragmentIndex = newIndex
    }

    private fun animateBounce(view: View) {
        val scale = ScaleAnimation(
            1f, 1.12f, 1f, 1.12f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 200
            interpolator = OvershootInterpolator()
            fillAfter = false
        }
        view.startAnimation(scale)
    }
}
