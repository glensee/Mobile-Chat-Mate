package com.example.chatmate

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.chatmate.databinding.ActivityMainBinding
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*


class MainActivity : AppCompatActivity() {

    // declaring binding for data binding
    private lateinit var binding:ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private var currentUUID = ""
    private var currentUser = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // set data binding
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore
//        FirebaseFirestore.setLoggingEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        // remove player from online players list in firebase
        Log.i("cliffen",currentUUID)
        db.collection("players").document(currentUUID)
            .delete()
            .addOnSuccessListener { Log.d("cliffen", "DocumentSnapshot successfully deleted!") }
            .addOnFailureListener { e -> Log.w("cliffen", "Error deleting document", e) }

        Log.i("cliffen", "past that stage")
    }

    fun NavigateToGame(view: View) {
        Log.e("cliffen", "dev mode")
        val it = Intent(this, GameActivity::class.java)
        startActivity(it)
    }

    fun NavigateToHome(view: View) {
        // get username and password
        val name = binding.name.text.toString().trim()

        // if username empty
        if (name.length <= 0) {
            Toast.makeText(this, "Please enter a username!", Toast.LENGTH_SHORT)
                .show()
        } else {
            val sharedPref = this.getSharedPreferences("usernames", Context.MODE_PRIVATE)
            val existingUUID = sharedPref.getString(name,  null)
            val uuid = UUID.randomUUID().toString()
            currentUser = name
//            Log.i("cliffen", existingUUID)

            // user doesn't exist in shared pref
            if (existingUUID == null) {
                Log.i("cliffen", "doesnt exist")
                with (sharedPref.edit()) {
                    putString(currentUser, uuid)
                    apply()
                }
                currentUUID = uuid
            } else {
                Log.i("cliffen", "exist")
                // if user has been created in shared pref before
                currentUUID = existingUUID
            }

            Log.i("cliffen", currentUUID)
         // Create online player in firebase
            val player = hashMapOf(
                "name" to currentUser,
                "id" to currentUUID
            )

            // Add a new document with a generated ID
            db.collection("players")
                .document(currentUUID)
                .set(player)
                .addOnSuccessListener { documentReference ->
                    Log.d("cliffen", "Document added!")
                }
                .addOnFailureListener { e ->
                    Log.d("cliffen", "Error adding document", e)
                }

            MediaPlayer.create(this, R.raw.ui_click).start()
            val it = Intent(this, LandingActivity::class.java)
            it.putExtra("name", currentUser)
            it.putExtra("uuid", currentUUID)
            startActivity(it)
        }
    }
}