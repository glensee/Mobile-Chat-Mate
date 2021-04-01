package com.example.chatmate
import android.app.Dialog
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chatmate.databinding.ActivityGameBinding
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.google.android.gms.tasks.Task
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.speechly.client.slu.Segment
import com.speechly.client.speech.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList


class GameActivity : AppCompatActivity() {
    private lateinit var gameBinding: ActivityGameBinding
    private lateinit var board: Board
    val speechlyClient: Client = Client.fromActivity(activity = this, appId = UUID.fromString("8a313e01-b0f3-4e6f-94a9-67cd65433135"))
    private lateinit var prevSegment: Segment
    // Keeps track of all generated Image Button Tiles
    private val chessTiles = ArrayList<ImageButton>()
    private var tileSelectedIndex = -1
    // List of Legal Moves for Current Turn
    private val currentLegalMoves = ArrayList<Move>()

    // Online Game Variables
    private lateinit var db: FirebaseFirestore
    private var isOnlineGame = false
    private lateinit var roomId: String
    private lateinit var localPlayerName: String
    private lateinit var localPlayerColor:Side
    private lateinit var onlinePlayerName: String
    private  var isOnlineGameIntialized = false
    private lateinit var finalSegment: Segment
    private lateinit var successListener: Task<DocumentSnapshot>
    private lateinit var snapshotListener: ListenerRegistration
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameBinding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(gameBinding.root)
        db = Firebase.firestore
        this.supportActionBar!!.hide()

        // Get Local Player Name
        localPlayerName = intent.getStringExtra("name").toString()

        // Assign Voice Command Listener to Button
        gameBinding.voiceCommandBtn.setOnTouchListener(voiceCommandButtonTouchListener)
        GlobalScope.launch(Dispatchers.Default) {
            speechlyClient.onSegmentChange { segment: Segment ->
                finalSegment = segment
                val transcript = segment.words.values.map{it.value}.joinToString(" ")
                Log.i("cliffen current segment", transcript)
            }
        }

        // Generate a new board
        board = Board()
        // Create Image button tiles in the layout
        generateChessBoardTileButtons()
        // Set the Image in the Image button based on pieces in board class
        renderBoardState()
        // Set All Current Legal Moves
        currentLegalMoves.addAll(board.legalMoves())

        //Check for Online Game
        roomId = intent.getStringExtra("roomId").toString()
        if(roomId != "null" && !isOnlineGameIntialized) {
            gameBinding.onlineGameHeader.visibility = View.VISIBLE
            setupOnlineGame()
        }
    }

    override fun onResume() {
        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        super.onResume()
    }

    override fun onPause() {
        mediaPlayer.stop()
        super.onPause()
    }

    private var voiceCommandButtonTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    gameBinding.voiceResultTextField.text = ""
                    speechlyClient.startContext()
                }
                MotionEvent.ACTION_UP -> {
                    speechlyClient.stopContext()
                    GlobalScope.launch(Dispatchers.Default) {
                        delay(1500)
                        val transcript = finalSegment.words.values.map{it.value}.joinToString(" ")
                        Log.i("cliffen final segment", transcript)
                        GlobalScope.launch(Dispatchers.Main) {
                            gameBinding.voiceResultTextField.text = transcript
                            Log.d("DEBUG", transcript)
                            try {
                                if (finalSegment.words.values.size >= 5) {
                                    movePieceWithVoiceCommand(transcript)
                                }
                            } catch(error: Error) {
                                Log.d("ERROR", error.toString())
                            }
                        }
                    }
                }
            }
            return true
        }
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
            gameBinding.chessBoardLinearLayout.addView(row)
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
                tile.setOnClickListener {
                    val tileIndex = it.tag.toString().toInt()
                    selectTileAtIndex(tileIndex)
                }
            }
        }
    }

    private fun renderBoardState () {
        Log.i("cliffen", "render 1")
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

            // Set the Background for Image Button
            if (Square.squareAt(i).isLightSquare) {
                chessTile.setBackgroundColor(ContextCompat.getColor(this, R.color.chess_light))
            } else {
                chessTile.setBackgroundColor(ContextCompat.getColor(this, R.color.chess_dark))
            }
        }

    }

    private fun selectTileAtIndex (tileIndex: Int) {
        val sideToMove = board.sideToMove
        if (isOnlineGame && localPlayerColor != sideToMove) {
            return
        }
        val pieceAtIndex = board.getPiece(Square.squareAt(tileIndex))

        // Check if a Chess Piece was Previously Selected
        if (tileSelectedIndex == -1) {
            // Chess Piece not Previously Selected
            // Check if Selected Tile has a Chess Piece Belonging to Current Side to Move
            if (pieceAtIndex != Piece.NONE && pieceAtIndex.pieceSide == sideToMove) {
                // Highlight Tile that was Selected
                chessTiles[tileIndex].setBackgroundColor(Color.parseColor("#F6F669"))
                // Save the Index of the Selected Tile
                tileSelectedIndex = tileIndex
                // Check for eligible moves
                val allLegalMovesCurrent = board.legalMoves()
                // iterating through all the legal moves on the board
                currentLegalMoves.clear()
                // add to list of piece's legal moves
                currentLegalMoves.addAll(allLegalMovesCurrent)
                for (eachMove in allLegalMovesCurrent) {
                    // if legal move is relevant to selected piece
                    if (Square.squareAt(tileSelectedIndex).toString().toLowerCase(Locale.ENGLISH) == eachMove.toString().substring(0,2)) {
                        // change colour of legal moves
                        chessTiles[Square.values().indexOf(eachMove.to)].setBackgroundColor(Color.parseColor("#48D1CC"))
                    }
                }
            }

        } else {
            // Check if Selected tile is same as previous
            if (tileIndex == tileSelectedIndex) {
                tileSelectedIndex = -1
                renderBoardState()
                return
            }
            // Check New Move Object
            val newMove = Move(Square.squareAt(tileSelectedIndex),Square.squareAt(tileIndex))
            // Check if New Move is Legal
            if(newMove in currentLegalMoves) {
                board.doMove(newMove)
                renderBoardState()
                tileSelectedIndex = -1
                afterMoveHandler()

                if (isOnlineGame){
                    sendBoardStateOnline()
                }
            }
        }
    }

    private fun movePieceWithVoiceCommand(command: String){
        val sideToMove = board.sideToMove
        if (isOnlineGame && localPlayerColor != sideToMove) {
            return
        }
        try {
            // Convert Command to a Move
            val commandSegments  = command.split(" TO ")
            if (commandSegments.size != 2) throw Error("Invalid Command")
            val from = Square.fromValue(commandSegments[0].split(" ")[0] + wordToNumber(commandSegments[0].split(" ")[1]))
            val to = Square.fromValue(commandSegments[1].split(" ")[0] + wordToNumber(commandSegments[1].split(" ")[1]))
            if (from !in Square.values()) throw Error("Invalid Command")
            if (to !in Square.values()) throw Error("Invalid Command")

            val newMove = Move(from, to)
            // Check if New Move is Legal
            if(newMove in currentLegalMoves) {
                board.doMove(newMove)
                renderBoardState()
                currentLegalMoves.clear()
                currentLegalMoves.addAll(board.legalMoves())
                gameBinding.voiceResultTextField.text = "Tap and hold on this side of the screen to speak"
                afterMoveHandler()
            } else {
                throw Error("Invalid Command")
            }
        } catch (error: Error) {
            Toast.makeText(this, "Invalid Command. Please Try Again", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            // Toast.makeText(this, "Invalid Command. Please Try Again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wordToNumber(word: String): Int {
        return when(word){
            "ONE" -> 1
            "TWO" -> 2
            "THREE" -> 3
            "FOUR" -> 4
            "FIVE" -> 5
            "SIX" -> 6
            "SEVEN" -> 7
            "EIGHT" -> 8
            else -> 0
        }

    }

    private fun afterMoveHandler() {
        val myDialog = Dialog(this)
        myDialog.setContentView(R.layout.game_finish_popup)
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.setCancelable(false)
        myDialog.findViewById<Button>(R.id.returnBtn).setOnClickListener{
//             remove firestore snapshot and success listener
            snapshotListener.remove()
            finish()
        }
        if (board.isMated) {
            myDialog.show()
            val result = "${board.sideToMove.flip().toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)} Wins"
            myDialog.findViewById<TextView>(R.id.resultText).setText(result)
            if(board.sideToMove == Side.BLACK){
                myDialog.findViewById<ShapeableImageView>(R.id.whiteAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
            } else {
                myDialog.findViewById<ShapeableImageView>(R.id.blackAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
            }
        } else if (board.isDraw) {
            if (board.isRepetition) {
                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
                myDialog.show()
            } else if (board.isInsufficientMaterial) {
                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
                myDialog.show()
            } else if (board.halfMoveCounter >= 100) {
                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
                myDialog.show()
            }
            else if (board.isStaleMate){
                myDialog.findViewById<TextView>(R.id.resultText).setText("Stale Mate")
                myDialog.show()
            }
        }
    }

    private fun setupOnlineGame() {
        isOnlineGame = true
        val roomRef = db.collection("rooms").document(roomId)

        // Initialize turn variable
        val turnData = hashMapOf("currentTurn" to board.sideToMove.toString())
        roomRef.set(turnData, SetOptions.merge())

        // Setup Headers
        roomRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val owner = document.data?.getValue("owner").toString()
                val player = document.data?.getValue("player").toString()
                if (owner == localPlayerName) {
                    localPlayerColor  = Side.WHITE
                    onlinePlayerName = player
                    gameBinding.onlineGameTitle.text = "Currently playing with $onlinePlayerName"
                    gameBinding.onlineGameTurnText.text = "Your (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                } else {
                    localPlayerColor  = Side.BLACK
                    onlinePlayerName = owner
                    gameBinding.onlineGameTitle.text = "Currently playing with $onlinePlayerName"
                    gameBinding.onlineGameTurnText.text = "${onlinePlayerName}'s (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                }
            }
        }

        snapshotListener = roomRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // On Board State Change
                val onlineBoardState = snapshot.data!!["boardState"].toString()
                if (onlineBoardState !== "null" && board.fen !== onlineBoardState) {
                    board.loadFromFen(onlineBoardState)
                    renderBoardState()
                    afterMoveHandler()
                    if (board.sideToMove == localPlayerColor) {
                        gameBinding.onlineGameTurnText.text = "Your (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                    } else {
                        gameBinding.onlineGameTurnText.text = "${onlinePlayerName}'s (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                    }
                    currentLegalMoves.clear()
                    currentLegalMoves.addAll(board.legalMoves())
                }
            }
        }

        isOnlineGameIntialized = true
    }

    private fun sendBoardStateOnline() {
        val roomRef = db.collection("rooms").document(roomId)
        val boardData = hashMapOf("boardState" to board.fen)
        roomRef.set(boardData, SetOptions.merge())
    }
}