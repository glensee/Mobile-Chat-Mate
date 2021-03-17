package com.example.chatmate

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.chatmate.databinding.ActivityLobbyBinding
import com.example.chatmate.databinding.ActivityRoomBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoomBinding
    private lateinit var db: FirebaseFirestore
    private var roomId = ""
    private var identity = ""
    private var owner = ""
    private var player = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // receive variables from intent
        roomId = intent.getStringExtra("roomId").toString()
        identity = intent.getStringExtra("identity").toString()
        // initialize firestore
        db = Firebase.firestore
        FirebaseFirestore.setLoggingEnabled(true)

        // initialize view text field bindings
        val viewOwner = binding.player1
        val viewPlayer = binding.player2
        val viewRoomId = binding.roomId
        val gameButton = binding.gameButton

        (viewRoomId.text.toString() + roomId).also { viewRoomId.text = it }

        // set button text depending on identity
        when (identity) {
            "owner" -> gameButton.text = "waiting for player"
            "player" -> gameButton.text = "ready"
        }


        // add listener to update owner and player
            val roomRef = db.collection("rooms").document(roomId)
        roomRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("cliffen", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d("cliffen", "Current data: ${snapshot.data}")
                owner = snapshot.data!!["owner"].toString()
                player = snapshot.data!!["player"].toString()

                (viewOwner.text.toString() + owner).also { viewOwner.text = it }
                (viewPlayer.text.toString() + player).also { viewPlayer.text = it }

            } else {
                Log.d("cliffen", "Current data: null")
            }
        }

    }
}