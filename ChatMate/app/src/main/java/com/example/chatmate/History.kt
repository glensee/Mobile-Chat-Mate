package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class History : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
    }
}