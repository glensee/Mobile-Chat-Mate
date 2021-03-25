package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import com.example.chatmate.databinding.ActivityLandingBinding


class LandingActivity : AppCompatActivity() {

    // declaring binding for data binding
    private lateinit var binding:ActivityLandingBinding
    var name = ""
    var uuid = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        // set data binding
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // display username
        name = intent.getStringExtra("name").toString()
        uuid = intent.getStringExtra("uuid").toString()

        val emptyGreeting = binding.greeting.text.toString()
        binding.greeting.text = emptyGreeting + name
    }

    fun NavigateToGame(view: View) {
        val it = Intent(this, GameActivity::class.java)
        it.putExtra("name", name)
        it.putExtra("uuid", uuid)
        startActivity(it)
    }

    fun NavigateToLobby(view: View) {
        val it = Intent(this, LobbyActivity::class.java)
        it.putExtra("name", name)
        it.putExtra("uuid", uuid)
        startActivity(it)
    }

    fun NavigateToHome(view: View) {
        val it = Intent(this, MainActivity::class.java)
        it.putExtra("name", name)
        it.putExtra("uuid", uuid)
        startActivity(it)
    }

}