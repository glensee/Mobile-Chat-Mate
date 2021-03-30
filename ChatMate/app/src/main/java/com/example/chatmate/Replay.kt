package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.chatmate.databinding.ActivityReplayBinding

class Replay : AppCompatActivity() {

    private lateinit var binding: ActivityReplayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityReplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}