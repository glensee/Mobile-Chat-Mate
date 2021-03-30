package com.example.chatmate

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.chatmate.databinding.ActivityLobbyBinding
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class LobbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLobbyBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var roomRef: CollectionReference
    private lateinit var roomsAdapter:ArrayAdapter<String>
    private var roomList = ArrayList<String>()
    private var roomIdList = ArrayList<String>()
    private var playerName = ""
    private var uuid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // receive name and uuid from intent
        playerName = intent.getStringExtra("name").toString()
        uuid = intent.getStringExtra("uuid").toString()

        Log.i("cliffen", "player name:" + playerName)

        // initialize firestore
        db = Firebase.firestore
        FirebaseFirestore.setLoggingEnabled(true)

        // listen to room changes
        roomRef = db.collection("rooms")
        roomRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("cliffen", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                var tempRoomList = ArrayList<String>()
                var tempRoomIdList = ArrayList<String>()
                var snapshotDocuments = snapshot.documents
                if (snapshotDocuments !== null) {
                    for (documentSnapshot in snapshotDocuments) {
                        val docSnap = documentSnapshot.data
                        if (docSnap != null) {
                            val owner = docSnap["owner"].toString()
                            val player =  docSnap["player"].toString()
                            val roomId = docSnap["roomId"].toString()
                            var roomDesc = ""

                            if (owner == "null") {
                                roomDesc = player + "'s room"
                            } else {
                                roomDesc = owner + "'s room"
                            }

                            tempRoomList.add(roomDesc)
                            tempRoomIdList.add(roomId)
                        }
                    }
                }

                // update list and id lists from firestore
                roomList = tempRoomList
                roomIdList = tempRoomIdList

                // update list view
                roomsAdapter = ArrayAdapter<String>(this, com.example.chatmate.R.layout.room_textview, roomList)
                binding.rooms.adapter = roomsAdapter

                // set on click listener for listview items
                binding.rooms.setOnItemClickListener{ _, _, index, _ ->

                // register player in firebase
                val roomId = roomIdList[index]
                val data = hashMapOf(
                    "player" to playerName,
                    "playerStatus" to "WAITING")
                db.collection("rooms").document(roomId)
                    .set(data, SetOptions.merge())

                // enter room
                val it = Intent(this, RoomActivity::class.java)
                it.putExtra("roomId", roomId)
                it.putExtra("identity", "player")
                it.putExtra("name", playerName)
                it.putExtra("uuid", uuid)
                    startActivity(it)
                }
                roomsAdapter.notifyDataSetChanged()

                Log.i("cliffen", roomList.toString())
            } else {
                Log.d("cliffen", "Current data: null")
            }
        }

    }
    
    fun createRoom(view: View) {
        val room = hashMapOf(
            "roomId" to uuid,
            "owner" to playerName,
            "ownerStatus" to "READY",
            "playerStatus" to "WAITING",
            "matchStarted" to false
        )

        db.collection("rooms").document(uuid)
            .set(room)
            .addOnSuccessListener { Log.i("cliffen", "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.i("cliffen", "Error writing document", e) }

        val it = Intent(this, RoomActivity::class.java)
        it.putExtra("roomId", uuid)
        it.putExtra("identity", "owner")
        it.putExtra("name", playerName)
        it.putExtra("uuid", uuid)
        startActivity(it)
    }
}