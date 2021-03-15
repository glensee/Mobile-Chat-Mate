package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RoomActivity : AppCompatActivity() {

    lateinit var playerName: String
    lateinit var roomName: String
    lateinit var db: FirebaseDatabase
    lateinit var  roomRef: DatabaseReference
    lateinit var roomsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)


        db = FirebaseDatabase.getInstance()
//        playerName =
    }
}