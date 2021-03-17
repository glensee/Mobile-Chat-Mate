package com.example.chatmate

import android.R
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.chatmate.databinding.ActivityRoomBinding
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class RoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoomBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var roomRef: CollectionReference
    private lateinit var roomsAdapter:ArrayAdapter<String>
    private var roomList = ArrayList<String>()
    private var playerName = ""
    private var uuid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomBinding.inflate(layoutInflater)
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
                var snapshotDocuments = snapshot.documents
                if (snapshotDocuments !== null) {
                    for (documentSnapshot in snapshotDocuments) {
                        val docSnap = documentSnapshot.data
                        if (docSnap != null) {
                            val player1 = docSnap["player1"].toString()
                            val player2 =  docSnap["player2"].toString()
                            val roomDesc = player1 + "'s room"
                            tempRoomList.add(roomDesc)
                        }
                    }
                }

                roomList = tempRoomList

                // update list view
                roomsAdapter = ArrayAdapter<String>(this, R.layout.simple_list_item_1, roomList)
                binding.rooms.adapter = roomsAdapter
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
            "player1" to playerName
        )

        db.collection("rooms").document(uuid)
            .set(room)
            .addOnSuccessListener { Log.i("cliffen", "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.i("cliffen", "Error writing document", e) }
    }
    
}