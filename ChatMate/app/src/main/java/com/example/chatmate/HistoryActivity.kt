package com.example.chatmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import com.example.chatmate.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var matchesAdapter: ArrayAdapter<String>
    private var matchesList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchesList.add("Eunice vs Eumean")
        matchesList.add("Cliffen vs Cliffun")
        matchesList.add("Glen vs Gland")
        matchesList.add("Arix vs Trex")
        matchesList.add("Mary vs Mury")
        matchesList.add("Bao xian vs Bao sweet")


        // create matches adapter
        matchesAdapter =
            ArrayAdapter<String>(this, com.example.chatmate.R.layout.matches_textview, matchesList)
        binding.matches.adapter = matchesAdapter

        // set on click listener for listview items
        binding.matches.setOnItemClickListener { _, _, index, _ ->
            Log.i("cliffen", "${matchesList[index]} clicked!")
        }
        matchesAdapter.notifyDataSetChanged()

    }
}