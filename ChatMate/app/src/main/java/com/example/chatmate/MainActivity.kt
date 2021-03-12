package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.chatmate.databinding.ActivityMainBinding
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    // declaring binding for data binding
    private lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // set data binding
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun NavigateToHome(view: View) {
        Log.e("cliffen", "hello")
        if (binding.name.text.toString().length <= 0) {
            Toast.makeText(this, "Please enter a name!", Toast.LENGTH_SHORT).show()
        } else {
            val it = Intent(this, HomeActivity::class.java)
            startActivity(it)
        }

    }


}