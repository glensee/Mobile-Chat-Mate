package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.chatmate.databinding.ActivityMainBinding
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import java.util.*
import com.speechly.client.slu.*
import com.speechly.client.speech.Client
import com.speechly.ui.SpeechlyButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // declaring binding for data binding
    private lateinit var binding:ActivityMainBinding

    // initializing speechly button and related variables
    private var button: SpeechlyButton? = null
    private var textView: TextView? = null
    private var positions: String? = null

    // creating speechly client
    val speechlyClient: Client = Client.fromActivity(activity = this, UUID.fromString("8a313e01-b0f3-4e6f-94a9-67cd65433135"))

    // setting on speechly on click listener
    private var buttonTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    textView?.visibility = View.VISIBLE
                    textView?.text = ""
                    speechlyClient.startContext()
                }
                MotionEvent.ACTION_UP -> {
                    speechlyClient.stopContext()
                    GlobalScope.launch(Dispatchers.Default) {
                        delay(500)
//                        textView?.visibility = View.INVISIBLE
                    }
                }
            }
            return true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        // set data binding
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // speechly
        this.button = binding.speechly
        this.textView = binding.inputText
        GlobalScope.launch(Dispatchers.Default) {
            speechlyClient.onSegmentChange { segment: Segment ->
                val transcript: String = segment.words.values.map{it.value}.joinToString(" ")
//                Log.i("segment", segment.words.values.toString())
                GlobalScope.launch(Dispatchers.Main) {
                    textView?.setText("${transcript}")
//                    Log.i("transcript",transcript)
                    if (segment.intent != null) {
//                        Log.i("intent", segment.intent.toString())
                        when(segment.intent?.intent) {
                            "move" -> {
                                positions = segment.getEntityByType("positions")?.value
//                                Log.i("positions", positions!!)
                            }
                        }
                    }
                }
            }
        }

        button?.setOnTouchListener(buttonTouchListener)
    }

    fun NavigateToGame(view: View) {
        val it = Intent(this, GameActivity::class.java)
        startActivity(it)
    }


}