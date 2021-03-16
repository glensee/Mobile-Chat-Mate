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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.FileOutputStream
import java.util.*

class LoginFragment : Fragment(), View.OnClickListener {

    // setting up variables for firebase
    lateinit var db: FirebaseFirestore

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
            db = Firebase.firestore
        }
    }

    override fun onClick(v: View) {
        val act = activity

        if (act != null) {
            // get username and password
            val username = act.findViewById<EditText>(R.id.name).text.toString().trim()

            // if username or password empty
            if (username.length <= 0) {
                Toast.makeText(act, "Please enter username and password!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                //TODO: check for validity of username and password
                val uuid = UUID.randomUUID()

                // Create a new user with a first and last name
                val player = hashMapOf(
                    "name" to username,
                    "id" to uuid
                )

                // Add a new document with a generated ID
                db.collection("users")
                    .add(player)
                    .addOnSuccessListener { documentReference ->
                        Log.d("cliffen", "DocumentSnapshot added with ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.w("cliffen", "Error adding document", e)
                    }
            }
        }
    }
}