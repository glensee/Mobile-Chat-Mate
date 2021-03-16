package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class RoomActivity : AppCompatActivity() {

    lateinit var playerName: String
    lateinit var roomName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
    }
}