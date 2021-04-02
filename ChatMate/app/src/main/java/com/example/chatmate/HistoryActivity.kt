package com.example.chatmate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import com.example.chatmate.databinding.ActivityHistoryBinding
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var matchesAdapter: ArrayAdapter<String>
    private lateinit var name: String
    private var matchesInfo = ArrayList<String>()
    private var matchesList = ArrayList<String>()

    private var TAG = "cliffen"

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        name = intent.getStringExtra("name").toString()

        try {
            val scan = Scanner(openFileInput("$name.txt"))
            while (scan.hasNextLine()) {
                val historyArray = scan.nextLine().toString().split("  ")
                val title = historyArray.get(0)

                matchesList.add(title)

            }
        } catch (error: Exception) {
            Log.i("cliffen", error.toString())
        }


        // create matches adapter
        matchesAdapter =
            ArrayAdapter<String>(this, com.example.chatmate.R.layout.matches_textview, matchesList)
        binding.matches.adapter = matchesAdapter

        // set on click listener for listview items
        binding.matches.setOnItemClickListener { _, _, index, _ ->
            val scanner = Scanner(openFileInput("$name.txt"))
            if (index == 0) {
                matchesInfo = scanner.nextLine().toString().split("  ") as ArrayList<String>
            } else {
                for (i in 0 until index-1) {
                    scanner.nextLine()
                }
                matchesInfo = scanner.nextLine().toString().split("  ") as ArrayList<String>
            }

            val it = Intent(this, ReplayActivity::class.java)
            it.putExtra("matchesInfo", matchesInfo)
            startActivity(it)
            Log.i("cliffen", "${matchesList[index]} clicked!")
        }
        matchesAdapter.notifyDataSetChanged()

    }
}