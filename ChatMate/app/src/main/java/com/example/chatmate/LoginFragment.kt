package com.example.chatmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.io.FileOutputStream

class LoginFragment : Fragment(), View.OnClickListener {

    // setting up variables for firebase
    lateinit var db: FirebaseDatabase
    lateinit var playerRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val act = activity
        if (act != null) {
            val loginButton = act.findViewById<Button>(R.id.enter)
            loginButton.setOnClickListener(this)
            db = FirebaseDatabase.getInstance()
        }
    }

    override fun onClick(v: View) {
        val act = activity

        if (act != null) {
            // get username and password
            val username = act.findViewById<EditText>(R.id.name).text.toString().trim()
            val password = act.findViewById<EditText>(R.id.password).text.toString().trim()

            // if username or password empty
            if (username.length <= 0 || password.length <= 0) {
                Toast.makeText(act, "Please enter username and password!", Toast.LENGTH_SHORT).show()
            } else {
                //TODO: check for validity of username and password

                // set button text to be "logging in"
                val loginButton = act.findViewById<Button>(R.id.enter)
                loginButton.text = "LOGGING IN"
                loginButton.isEnabled = false


                // create player object in firebase
                playerRef = db.getReference("players/" + username)
                addEventListener()
                playerRef.setValue("")


            }
        }
    }

    fun addEventListener() {
        // read from db
        val act = activity
        playerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue<String>()
                Log.e("success", "Value is: $value")

                if (act != null) {
                    // set button text to be "logging in"
                    val loginButton = act.findViewById<Button>(R.id.enter)
                    loginButton.text = "LOGIN"
                    loginButton.isEnabled = true

                    // write username to shared preferences
                    val sharedPref = act.getPreferences(Context.MODE_PRIVATE) ?: return
                    with (sharedPref.edit()) {
                        putString("username", act.findViewById<EditText>(R.id.name).text.toString().trim())
                        apply()
                    }
                }

                // navigate to home page
                val it = Intent(act, HomeActivity::class.java)
                startActivity(it)

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.e("error", "Failed to read value.", error.toException())
                // set button text to be "logging in"
                if (act != null) {
                    val loginButton = act.findViewById<Button>(R.id.enter)
                    loginButton.text = "LOGIN"
                    loginButton.isEnabled = true
                }
            }

        })



    }

}