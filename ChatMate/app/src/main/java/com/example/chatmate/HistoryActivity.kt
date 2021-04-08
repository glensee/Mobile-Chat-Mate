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
    private var matchesInfo = ArrayList<ArrayList<String>>()
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
                val historyArray = scan.nextLine().toString().split("  ") as ArrayList<String>
                val title = historyArray[0]
                matchesInfo.add(historyArray)
                matchesList.add(title)

            }
        } catch (error: Exception) {
            Log.i(TAG, error.toString())
        }


        // create matches adapter
        matchesAdapter =
            ArrayAdapter<String>(this, com.example.chatmate.R.layout.matches_textview, matchesList)
        binding.matches.adapter = matchesAdapter

        // set on click listener for listview items
        binding.matches.setOnItemClickListener { _, _, index, _ ->
            val it = Intent(this, ReplayActivity::class.java)
            it.putExtra("matchesInfo", matchesInfo[index])
            startActivity(it)
        }
        matchesAdapter.notifyDataSetChanged()

    }
}