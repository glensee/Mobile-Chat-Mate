package com.example.chatmate

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.chatmate.databinding.ActivityGameBinding
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move


class GameActivity : AppCompatActivity() {
    lateinit var gameBinding: ActivityGameBinding
    lateinit var board: Board
    // Keeps track of all generated Image Button Tiles
    val chessTiles = ArrayList<ImageButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameBinding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(gameBinding.root)

        // Generate a new board
        board = Board()
        // Create Image button tiles in the layout
        generateChessBoardTileButtons()
        // Set the Image in the Image button based on pieces in board class
        renderBoardState()


        board.doMove(Move(Square.E2, Square.E4))
        renderBoardState()
    }

    private fun generateChessBoardTileButtons () {
        // Clear existing Image Button References
        chessTiles.clear()
        // Temporary Track the Sequence of Image Button Generation
        val boardArrayList = ArrayList<ArrayList<ImageButton>>()
        // LayoutParams used to "Style" the Generated Image Buttons & Linear Layouts
        var params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)

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

                // Set Background Color of the Image Button to Create the Chess Board Look
                if (rowIndex % 2 == 0) {
                    if (colIndex % 2 == 0) {
                        chessTile.setBackgroundColor(Color.parseColor("#EEEED2"))
                    } else {
                        chessTile.setBackgroundColor(Color.parseColor("#769656"))
                    }
                } else {
                    if (colIndex % 2 == 0) {
                        chessTile.setBackgroundColor(Color.parseColor("#769656"))
                    } else {
                        chessTile.setBackgroundColor(Color.parseColor("#EEEED2"))
                    }
                }

                // Insert Tile into ArrayList to keep track of its Generating Sequence
                rowArrayList.add(chessTile)
                // Insert Tile into the Linear Layout
                row.addView(chessTile)
            }
            // Insert Row into the Main Linear Layout of the Game Activity
            gameBinding.chessBoardLinearLayout.addView(row)
            // Insert Row into ArrayList to keep track of its Generating Sequence
            boardArrayList.add(rowArrayList)
        }

        // Using the Temporary Sequence, Add Tiles into the Main ArrayList That Tracks each Image Button
        // The Final Sequence matches the Sequence Used in the Board Class
        for(i in boardArrayList.size - 1 downTo 0){
            for (tile in boardArrayList[i]) {
                chessTiles.add(tile)
            }
        }
    }

    private fun renderBoardState () {
        // Get the Current State of the Chess Board in an Array Sequence
        // Each Index Represents a Tile on the Board and The Item Represents the Piece Type
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
        }

        gameBinding.textView.text = board.toString()
    }
}