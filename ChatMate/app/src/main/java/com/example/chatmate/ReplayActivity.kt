package com.example.chatmate

import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.example.chatmate.databinding.ActivityReplayBinding
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import java.util.*
import kotlin.collections.ArrayList

class ReplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReplayBinding
    private lateinit var board: Board
    // Keeps track of all generated Image Button Tiles
    private val chessTiles = ArrayList<ImageButton>()
    private var tileSelectedIndex = -1
    private var matchesInfo = ArrayList<String>()
    private var TAG = "cliffen"
    private var boardHistory = ArrayList<String>()
    private var winner = ""
    private var title = ""
    private lateinit var localPlayerColor:Side


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityReplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.supportActionBar!!.hide()

        // retrieving match info from intent
        matchesInfo = intent.getStringArrayListExtra("matchesInfo") as ArrayList<String>

        // assigning variables to be displayed
        title = matchesInfo[0]
        val boardHistoryString = matchesInfo[1].toString().trim('[').trim(']')
        Log.i(TAG, boardHistoryString)
        if (boardHistoryString.contains(", ")) {
            boardHistory = boardHistoryString.split(", ") as ArrayList<String>
        } else {
            boardHistory.add(boardHistoryString)
        }
        winner = matchesInfo[2].trim(' ')
        Log.i(TAG, "history: $boardHistory")
        Log.i(TAG, matchesInfo.toString())

        // Setup the initial board
        board = Board()

        // Create Image button tiles in the layout
        generateChessBoardTileButtons()
        // Set the Image in the Image button based on pieces in board class
        renderBoardState(0)

        // display player names
        if (title == "Local game") {
            binding.player1.text = "White"
            binding.player2.text = "Black"
        } else {
            val playerArray = title.split(" vs ") as ArrayList<String>
            binding.player1.text = playerArray.get(0)
            binding.player2.text = playerArray.get(1)
        }

        // display winner tag
        if (winner == "White") {
            binding.trophy1.visibility = View.VISIBLE
            binding.trophy2.visibility = View.GONE
        } else if (winner == "Black") {
            binding.trophy1.visibility = View.GONE
            binding.trophy2.visibility = View.VISIBLE
        }

        // adding seekbar value change listener
        val seekbar = binding.seekbar
        seekbar.min = 0
        seekbar.max = boardHistory.size -1
        seekbar.progress = 0

        binding.moves.text = "Moves: ${seekbar.progress}/${seekbar.max}"

        seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.moves.text = "Moves: ${seekbar.progress}/${seekbar.max}"
                renderBoardState(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun generateChessBoardTileButtons () {
        // Clear existing Image Button References
        chessTiles.clear()
        // Temporary Track the Sequence of Image Button Generation
        val boardArrayList = ArrayList<ArrayList<ImageButton>>()
        // LayoutParams used to "Style" the Generated Image Buttons & Linear Layouts
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)

        // Generate Image Button by Rows
        for (rowIndex in 0 until 8) {
            // Create Linear Layout to Hold a Row of Image Buttons
            val row = LinearLayout(this)
            // Temporary Track the Sequence of Image Button Generation for a Row
            val rowArrayList = ArrayList<ImageButton>()
            // Set the LinearLayout to have the "Style" of params
            row.layoutParams = params

            // Generate Image Button by Rows
            for (colIndex in 0 until 8) {
                // Create Image Button, This Represents a Tile on the Board
                val chessTile = ImageButton(this)
                // Set the Image Button to have the "Style" of params
                chessTile.layoutParams = params
                // Set the Image in the Image Button to Fit Nicely
                chessTile.scaleType = ImageView.ScaleType.FIT_XY
                // Set Padding Within the Image Button
                chessTile.setPadding(25,25,25,25)
                // Insert Tile into ArrayList to keep track of its Generating Sequence
                rowArrayList.add(chessTile)
                // Insert Tile into the Linear Layout
                row.addView(chessTile)
            }
            // Insert Row into the Main Linear Layout of the Game Activity
            binding.chessBoardLinearLayout.addView(row)
            // Insert Row into ArrayList to keep track of its Generating Sequence
            boardArrayList.add(rowArrayList)
        }

        // Using the Temporary Sequence, Add Tiles into the Main ArrayList That Tracks each Image Button
        // The Final Sequence matches the Sequence Used in the Board Class
        for (i in boardArrayList.size - 1 downTo 0) {
            var index = i
            if (this::localPlayerColor.isInitialized && localPlayerColor == Side.BLACK) {
                index = boardArrayList.size - 1 - i
            }
            for (tile in boardArrayList[index]) {
                tile.tag = chessTiles.size
                chessTiles.add(tile)
                }
            }
        }

    private fun renderBoardState (state: Int) {
        Log.i("cliffen", "render 1")
        // Get the Current State of the Chess Board in an Array Sequence
        // Each Index Represents a Tile on the Board and The Item Represents the Piece Type
        board.loadFromFen(boardHistory[state])
        val currentBoardStateArray = board.boardToArray()

        for (i in 0 until 64) {

            // Image Button to be Rendered on
            val chessTile = chessTiles[i]

            // Render Image Base on Piece Value
            when (currentBoardStateArray[i].value()) {
                "WHITE_ROOK" -> chessTile.setImageResource(R.drawable.w_rook_2x_ns)
                "WHITE_KNIGHT" -> chessTile.setImageResource(R.drawable.w_knight_2x_ns)
                "WHITE_BISHOP" -> chessTile.setImageResource(R.drawable.w_bishop_2x_ns)
                "WHITE_QUEEN" -> chessTile.setImageResource(R.drawable.w_queen_2x_ns)
                "WHITE_KING" -> chessTile.setImageResource(R.drawable.w_king_2x_ns)
                "WHITE_PAWN" -> chessTile.setImageResource(R.drawable.w_pawn_2x_ns)
                "BLACK_PAWN" -> chessTile.setImageResource(R.drawable.b_pawn_2x_ns)
                "BLACK_ROOK" -> chessTile.setImageResource(R.drawable.b_rook_2x_ns)
                "BLACK_KNIGHT" -> chessTile.setImageResource(R.drawable.b_knight_2x_ns)
                "BLACK_BISHOP" -> chessTile.setImageResource(R.drawable.b_bishop_2x_ns)
                "BLACK_QUEEN" -> chessTile.setImageResource(R.drawable.b_queen_2x_ns)
                "BLACK_KING" -> chessTile.setImageResource(R.drawable.b_king_2x_ns)
                else -> chessTile.setImageResource(0)
            }

            // Set the Background for Image Button
            if (Square.squareAt(i).isLightSquare) {
                chessTile.setBackgroundColor(ContextCompat.getColor(this, R.color.chess_light))
            } else {
                chessTile.setBackgroundColor(ContextCompat.getColor(this, R.color.chess_dark))
            }
        }

    }

    private fun nextMove(view: View) {

    }

    private fun prevMove(view: View) {

    }

    private fun leaveRoom(view: View) {

    }
}