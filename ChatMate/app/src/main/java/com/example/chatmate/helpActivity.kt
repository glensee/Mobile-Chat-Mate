package com.example.chatmate

import android.os.Bundle
import android.view.View
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

class helpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
    }

    fun flipView(view: View) {
        val flipper = findViewById<ViewFlipper>(R.id.viewFlipperHelp)
        flipper.showNext()
    }
}