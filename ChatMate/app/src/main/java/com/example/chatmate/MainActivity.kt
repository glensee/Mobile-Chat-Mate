package com.example.chatmate

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatmate.databinding.ActivityMainBinding
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

        binding.name.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                Log.i("glen", "here")
                NavigateToHome(v)
                return@OnKeyListener true
            }
            false
        })
    }

    fun NavigateToHome(view: View) {
        // get username and password
        val name = binding.name.text.toString().trim()
        // if username empty
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "Please enter a username!", Toast.LENGTH_SHORT)
                    .show()
            }
            name.contains(" ") -> {
                Toast.makeText(this, "Spaces in name not allowed!", Toast.LENGTH_SHORT).show()
            }
            name.length > 15 -> {
                Toast.makeText(this, "Maximum 15 characters!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val sharedPref = this.getSharedPreferences("usernames", Context.MODE_PRIVATE)
                val existingUUID = sharedPref.getString(name,  null)
                val uuid = UUID.randomUUID().toString()
                currentUser = name

                // user doesn't exist in shared pref
                currentUUID = if (existingUUID == null) {
                    with (sharedPref.edit()) {
                        putString(currentUser, uuid)
                        apply()
                    }
                    uuid
                } else {
                    // if user has been created in shared pref before
                    existingUUID
                }
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
}