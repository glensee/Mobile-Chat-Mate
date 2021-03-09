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

    // setting request code for android speech recognition intent
    companion object {
        private const val REQUEST_CODE_STT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // set data binding
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // android speech recognition
        binding.btnStt.setOnClickListener {
            // Get the Intent action
            val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            // Language model defines the purpose, there are special models for other use cases, like search.
            sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Adding an extra language, you can use any language from the Locale class.
            sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // Text that shows up on the Speech input prompt.
            sttIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now!")
            try {
                // Start the intent for a result, and pass in our request code.
                startActivityForResult(sttIntent, REQUEST_CODE_STT)
            } catch (e: ActivityNotFoundException) {
                // Handling error when the service is not available.
                e.printStackTrace()
                Toast.makeText(this, "Your device does not support STT.", Toast.LENGTH_LONG).show()
            }
        }

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

    // overriding method to handle intent from android speech recognition
    // displays recognized text in text view "inputText"
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Handle the result for our request code.
            REQUEST_CODE_STT -> {
                // Safety checks to ensure data is available.
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Retrieve the result array.
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    // Ensure result array is not null or empty to avoid errors.
                    if (!result.isNullOrEmpty()) {
                        // Recognized text is in the first position.
                        val recognizedText = result[0]
                        // Do what you want with the recognized text.
                        binding.inputText.text = recognizedText.toString()
                    }
                }
            }
        }
    }

    fun NavigateToGame(view: View) {
        val it = Intent(this, GameActivity::class.java)
        startActivity(it)
    }


}