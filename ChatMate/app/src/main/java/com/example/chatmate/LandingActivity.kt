package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.media.MediaPlayer
import android.view.View
import com.example.chatmate.databinding.ActivityLandingBinding
import java.lang.Exception
import java.lang.Math.floor
import java.lang.Math.round
import java.time.LocalDate
import java.time.Period
import java.util.*


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

        loadPreviousMatches()
    }

    private fun loadPreviousMatches() {
        // read match history and load values for win rate and games played in the past week
        try {
            val scan = Scanner(openFileInput("$name.txt"))
            val today = LocalDate.now()
            var gamesPastWeek = 0
            var gamesWon = 0f
            var gamesTotal = 0f

            while (scan.hasNextLine()) {
                val historyArray = scan.nextLine().toString().split("  ")
                val title = historyArray.get(0)
                val date = LocalDate.parse(historyArray.get(historyArray.size -1))

                // calculate number of games played in the last week
                if (Period.between(today, date).days <= 7) {
                    gamesPastWeek ++
                }

                // calculate the win rate of player
                if (title != "Local game") {
                    Log.i("cliffen", "first item: $title")
                    Log.i("cliffen", "im in local game")
                    val winnerColour = historyArray.get(historyArray.size -2)
                    var winner = ""

                    if (winnerColour == "Black") {
                        winner = title.split(" vs ")[1]
                    } else if (winnerColour == "White") {
                        winner = title.split(" vs ")[0]
                    }

                    if (name == winner) {
                        gamesWon ++
                    }
                }

                gamesTotal ++
            }

            var winRate = 0
            if (gamesTotal !== 0f && gamesWon !== 0f) {
                winRate = round(gamesWon/gamesTotal*100)
            }
            Log.i("cliffen", "winrate: $winRate number of matches: $gamesPastWeek")
            binding.winRate.text = "$winRate%"
            binding.numberOfMatches.text = "$gamesPastWeek"

        } catch (error: Exception) {
            Log.i("cliffen", error.toString())
        }
    }

    fun NavigateToGame(view: View) {
        MediaPlayer.create(this, R.raw.ui_click).start()
        val it = Intent(this, GameActivity::class.java)
        it.putExtra("name", name)
        it.putExtra("uuid", uuid)
        startActivity(it)
    }

    fun NavigateToLobby(view: View) {
        MediaPlayer.create(this, R.raw.ui_click).start()
        val it = Intent(this, LobbyActivity::class.java)
        it.putExtra("name", name)
        it.putExtra("uuid", uuid)
        startActivity(it)
    }

    fun NavigateToHome(view: View) {
        MediaPlayer.create(this, R.raw.ui_click).start()
        finish()
    }

    fun NavigateToHistory(view: View) {
        MediaPlayer.create(this, R.raw.ui_click).start()
        val it = Intent(this, HistoryActivity::class.java)
        it.putExtra("name", name)
        startActivity(it)
    }

    override fun onResume() {
        super.onResume()
        loadPreviousMatches()
    }

    fun goToHelp(view: View) {
        val it = Intent(this, helpActivity::class.java)
        startActivity(it)
    }
}