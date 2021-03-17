package com.example.chatmate

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.chatmate.databinding.ActivityLobbyBinding
import com.example.chatmate.databinding.ActivityRoomBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoomBinding
    private lateinit var db: FirebaseFirestore
    private var roomId = ""
    private var identity = ""
    private var owner = ""
    private var player = ""
    private var ownerStatus = ""
    private var playerStatus = ""
    private var matchStarted = false

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
        val viewOwnerStatus = binding.player1Status
        val viewPlayerStatus = binding.player2Status
//        val viewRoomId = binding.roomId
        val gameButton = binding.gameButton

//        (viewRoomId.text.toString() + roomId).also { viewRoomId.text = it }

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
                ownerStatus = snapshot.data!!["ownerStatus"].toString()
                playerStatus = snapshot.data!!["playerStatus"].toString()
                matchStarted = snapshot.data!!["matchStarted"].toString().toBoolean()

                viewOwner.text = "Player 1: " + owner
                viewPlayer.text = "Player2: " + player
                viewOwnerStatus.text = "Status: " + ownerStatus
                viewPlayerStatus.text = "Status: " + playerStatus

                // change gameButton text
                if (playerStatus == "ready" && identity == "owner") {
                    gameButton.text = "start game"
                }

                if (identity == "player") {
                    if (playerStatus == "ready") {
                        gameButton.text = "unready"
                    } else {
                        gameButton.text = "ready"
                    }
                }

                // start game if both ready
                if (ownerStatus == "ready" && playerStatus == "ready" && matchStarted) {
                    val it = Intent(this, GameActivity::class.java)
                    it.putExtra("roomId", roomId)
                    startActivity(it)
                }


            } else {
                Log.d("cliffen", "Current data: null")
            }
        }

    }

    fun startGame (view: View) {
        when (identity) {
            "player" -> {
                // set player status to ready if previously not
                var status = ""
                if (playerStatus == "not ready") {
                    status = "ready"
                } else {
                    status = "not ready"
                }
                val data = hashMapOf("playerStatus" to status)
                db.collection("rooms").document(roomId)
                        .set(data, SetOptions.merge())
            }

            "owner" -> {
                // start the match if player ready
                if (playerStatus == "ready") {
                    val data = hashMapOf("matchStarted" to true)
                    db.collection("rooms").document(roomId)
                            .set(data, SetOptions.merge())
                } else {
                    Toast.makeText(this, "All players must be ready!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}