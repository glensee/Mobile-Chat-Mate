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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private lateinit var snapshotListener: ListenerRegistration

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
                gameButton.text = "Waiting for player..."
                greeting.text = "Hang tight! We’re waiting for another ChatMater..."
            }
            "player" -> {
                gameButton.text = "Ready"
                binding.leaveRoom.visibility = View.VISIBLE
            }
        }


        // add listener to update owner and player
            val roomRef = db.collection("rooms").document(roomId)

        snapshotListener = roomRef.addSnapshotListener { snapshot, e ->
            Log.i("cliffen debug", roomRef.toString())
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
                    viewOwnerStatus.setTextColor(Color.parseColor("#B0A64C"))
                    Toast.makeText(this, "room closed by owner", Toast.LENGTH_SHORT).show()
                    snapshotListener.remove()
                    finish()
                }

                // check if player exists
                if (snapshot.get("player") !== null) {
                    player = snapshot.data!!["player"].toString()
                    playerStatus = snapshot.data!!["playerStatus"].toString()
                } else {
                    player = ""
                    playerStatus = "WAITING"
                    viewPlayerStatus.setTextColor(Color.parseColor("#B0A64C"))

                    if (identity == "owner") {
                        greeting.text = "Hang tight! We’re waiting for another ChatMater..."
                    }
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
                    viewOwnerStatus.setTextColor(Color.parseColor("#B0A64C"))

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
                    if (identity == "owner") {
                        greeting.text = "Hang tight! We’re waiting for another ChatMater..."
                    }
                    viewPlayer.text = "Player 2: "
                    viewPlayerStatus.text = "WAITING"
                    viewPlayerStatus.setTextColor(Color.parseColor("#B0A64C"))
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
                        gameButton.setBackgroundColor(Color.parseColor("#EEECF1"))
                        gameButton.setEnabled(true);
                    } else {
                        gameButton.text = "Waiting for player..."
                        gameButton.setBackgroundColor(Color.parseColor("#808080"))
                        gameButton.setEnabled(false);
                    }
                }

                if (ownerStatus == "READY" && playerStatus == "READY" && matchStarted) {
                    Log.i("cliffen", "i started one time")
                    snapshotListener.remove()
                    val it = Intent(this, GameActivity::class.java)
                    it.putExtra("roomId", roomId)
                    it.putExtra("name", name)
                    it.putExtra("identity", identity)
                    startActivity(it)
                }


            } else {
                Log.d("cliffen", "Room is gone")
            }
        }

    }

    fun startGame (view: View) {

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
                    // validation to alert owner when attempting to start without player being ready
                    Log.i("cliffen", "null name: " + player)
                    Toast.makeText(this, "All players must be ready!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun leaveRoom (view: View) {
        val docRef = db.collection("rooms").document(roomId)
        if (identity == "player") {

            // Remove the 'player' field from the document
            val updates = hashMapOf<String, Any>(
                "player" to FieldValue.delete()
            )

            docRef.update(updates).addOnCompleteListener { }
            snapshotListener.remove()
            finish()
        } else {
            // remove 'owner' field from document to let player know owner left
            val updates = hashMapOf<String, Any>(
                "owner" to FieldValue.delete()
            )
            docRef.update(updates).addOnCompleteListener { }

            // delete room from firestore
            db.collection("rooms").document(roomId)
                .delete()
                .addOnSuccessListener { Log.d("cliffen", "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w("cliffen", "Error deleting document", e) }
        }
    }

    private fun leaveRoom () {
        val docRef = db.collection("rooms").document(roomId)
        if (identity == "player") {

            // Remove the 'player' field from the document
            val updates = hashMapOf<String, Any>(
                "player" to FieldValue.delete()
            )

            docRef.update(updates).addOnCompleteListener { }
            snapshotListener.remove()
            finish()

        } else {
            // remove 'owner' field from document to let player know owner left
            val updates = hashMapOf<String, Any>(
                "owner" to FieldValue.delete()
            )
            docRef.update(updates).addOnCompleteListener { }

            // delete room from firestore
            db.collection("rooms").document(roomId)
                .delete()
                .addOnSuccessListener { Log.d("cliffen", "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w("cliffen", "Error deleting document", e) }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i("cliffen", "starting onStop...")
        if (!matchStarted) {
            leaveRoom()
        }
        Log.i("cliffen", "onstop ended")

    }

}