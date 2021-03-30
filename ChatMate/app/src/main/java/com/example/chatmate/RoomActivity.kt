package com.example.chatmate

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
    private var name = ""
    private var uuid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // receive variables from intent
        roomId = intent.getStringExtra("roomId").toString()
        identity = intent.getStringExtra("identity").toString()
        name = intent.getStringExtra("name").toString()
        uuid = intent.getStringExtra("uuid").toString()

        // initialize firestore
        db = Firebase.firestore
        FirebaseFirestore.setLoggingEnabled(true)

        // initialize view text field bindings
        val viewOwner = binding.player1
        val viewPlayer = binding.player2
        val viewOwnerStatus = binding.player1Status
        val viewPlayerStatus = binding.player2Status
        val greeting = binding.greeting
//        val viewRoomId = binding.roomId
        val gameButton = binding.gameButton

//        (viewRoomId.text.toString() + roomId).also { viewRoomId.text = it }

        // set button text depending on identity
        when (identity) {
            "owner" -> {
                gameButton.text = "waiting for player"
                greeting.text = "Hang tight! We’re waiting for another ChatMater..."
            }
            "player" -> {
                gameButton.text = "Ready"
            }
        }


        // add listener to update owner and player
            val roomRef = db.collection("rooms").document(roomId)
        roomRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("cliffen", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.i("cliffen", "Current data: ${snapshot.data}")

                // check if owner exists
                if (snapshot.get("owner") !== null) {
                    owner = snapshot.data!!["owner"].toString()
                    ownerStatus = snapshot.data!!["ownerStatus"].toString()
                } else {
                    owner = ""
                    ownerStatus = "WAITING"
                }

                // check if player exists
                if (snapshot.get("player") !== null) {
                    player = snapshot.data!!["player"].toString()
                    playerStatus = snapshot.data!!["playerStatus"].toString()
                } else {
                    player = ""
                    playerStatus = "WAITING"
                }

                // update match started value
                matchStarted = snapshot.data!!["matchStarted"].toString().toBoolean()

                // if owner exists
                if (owner !== "") {
                    if (identity == "player") {
                        greeting.text = "Would you like to duel with $owner in a chess game?"
                    }
                    viewOwner.text = "Player 1: " + owner
                    viewOwnerStatus.text = ownerStatus

                    when (ownerStatus) {
                        "READY" -> viewOwnerStatus.setTextColor(Color.parseColor("#84B04C"))
                        "WAITING" -> viewOwnerStatus.setTextColor(Color.parseColor("#B0A64C"))
                    }

                } else {
                    viewOwner.text = "Player 1: "
                    viewOwnerStatus.text = "WAITING"
                }

                // if player exists
                if (player !== "") {

                    // change greeting text
                    if (identity == "owner") {
                        greeting.text = "Would you like to duel with $player in a chess game?"
                    }

                    viewPlayer.text = "Player2: " + player
                    viewPlayerStatus.text = playerStatus

                    when (playerStatus) {
                        "READY" -> viewPlayerStatus.setTextColor(Color.parseColor("#84B04C"))
                        "WAITING" -> viewPlayerStatus.setTextColor(Color.parseColor("#B0A64C"))
                    }

                } else {
                    viewPlayer.text = "Player 2: "
                    viewPlayerStatus.text = "WAITING"
                }

                // change button text from if person is a player
                if (identity == "player") {
                    if (playerStatus == "READY") {
                        gameButton.text = "Unready"
                    } else {
                        gameButton.text = "Yes, I'm ready"
                    }
                }

                // change button text if person is a room owner
                if (identity == "owner") {
                    if (ownerStatus == "READY" && playerStatus == "READY") {
                        gameButton.text = "Start the game!"
                    } else {
                        gameButton.text = "Leave"
                    }
                }

                // start game if both ready
                if (ownerStatus == "READY" && playerStatus == "READY" && matchStarted) {
                    val it = Intent(this, GameActivity::class.java)
                    it.putExtra("roomId", roomId)
                    it.putExtra("name", name)
                    startActivity(it)
                }


            } else {
                // leave activity if room is closed
                finish()
                Log.d("cliffen", "Current data: null")
            }
        }

    }

    fun startGame (view: View) {
//        Log.i("cliffen","clicked!")
//        Log.i("cliffen","identity: $identity")
//        Log.i("cliffen","playerStatus: $playerStatus")
//        Log.i("cliffen","ownerStatus: $ownerStatus")
//        Log.i("cliffen","player: $player")

        when (identity) {

            "player" -> {
                // set player status to ready if previously not
                var status = ""
                if (playerStatus == "WAITING") {
                    status = "READY"
                } else {
                    status = "WAITING"
                }
                val data = hashMapOf("playerStatus" to status)
                db.collection("rooms").document(roomId)
                        .set(data, SetOptions.merge())
            }

            "owner" -> {
                // start the match if player ready
                if (playerStatus == "READY") {
                    val data = hashMapOf("matchStarted" to true)
                    db.collection("rooms").document(roomId)
                            .set(data, SetOptions.merge())
                } else {
                    // leave room

                    // delete room from firestore
                    db.collection("rooms").document(roomId)
                        .delete()
                        .addOnSuccessListener { Log.d("cliffen", "DocumentSnapshot successfully deleted!") }
                        .addOnFailureListener { e -> Log.w("cliffen", "Error deleting document", e) }

                    // return to lobby
                    finish()
                }
//                else {
//                    // validation to alert owner when attempting to start without player being ready
//                    Log.i("cliffen", "null name: " + player)
//                    Toast.makeText(this, "All players must be ready!", Toast.LENGTH_SHORT).show()
//                }
            }
        }
    }
}