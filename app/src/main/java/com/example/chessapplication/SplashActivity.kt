package com.example.chessapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Build the engine tables on the engine thread while the splash is showing.
        GameController.start()

        // Hide action bar if present
        supportActionBar?.hide()

        // Delay for 1.5 seconds then transition to MainActivity
        lifecycleScope.launch {
            delay(1500L)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()

        }
    }
}
