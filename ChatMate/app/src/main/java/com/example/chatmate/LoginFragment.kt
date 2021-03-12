package com.example.chatmate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LoginFragment : Fragment(), View.OnClickListener {


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
        }
    }

    override fun onClick(v: View) {
        val act = activity

        if (act != null) {
            if (act.findViewById<EditText>(R.id.name).text.toString().length <= 0 || act.findViewById<EditText>(
                    R.id.password
                ).text.toString().length <= 0
            ) {
                Log.e("cliffen", "No name entered")
                Toast.makeText(act, "Please enter username and password!", Toast.LENGTH_SHORT).show()
            } else {
                Log.i("cliffen", "we made it!!")
                val it = Intent(act, HomeActivity::class.java)
                startActivity(it)
            }
        } else {
            Log.e("cliffen", "activity is null")
        }
    }

}