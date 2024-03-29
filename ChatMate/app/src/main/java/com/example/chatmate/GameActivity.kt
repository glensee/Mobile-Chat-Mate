package com.example.chatmate
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chatmate.databinding.ActivityGameBinding
import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
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
import com.speechly.client.speech.NoActiveStreamException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.KeyStore
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.Exception
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class GameActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var gameBinding: ActivityGameBinding
    private lateinit var board: Board
    private lateinit var moveList: MoveList
    val speechlyClient: Client = Client.fromActivity(activity = this, appId = UUID.fromString("8a313e01-b0f3-4e6f-94a9-67cd65433135"))
    private lateinit var prevSegment: Segment
    // Keeps track of all generated Image Button Tiles
    private val chessTiles = ArrayList<ImageButton>()
    private var tileSelectedIndex = -1
    // List of Legal Moves for Current Turn
    private val currentLegalMoves = ArrayList<Move>()
    private var moveSanList = ArrayList<String>()
    // check if the game started with the first move
    private var firstMove = true
    // Hidden Board
    private var isBoardHidden = false

    // Online Game Variables
    private lateinit var db: FirebaseFirestore
    private var isOnlineGame = false
    private lateinit var roomId: String
    private lateinit var localPlayerName: String
    private lateinit var opponent: String
    private lateinit var localPlayerColor:Side
    private lateinit var onlinePlayerName: String
    private var isOnlineGameIntialized = false
    private lateinit var finalSegment: Segment
    private lateinit var successListener: Task<DocumentSnapshot>
    private lateinit var snapshotListener: ListenerRegistration
    private lateinit var boardHistory: ArrayList<String>
    private var boardHistoryLocal = ArrayList<String>()
    private lateinit var identity: String
    private var roomLeft = false
    private var boardSaved = false
    private var boardMoves = ArrayList<String>()
    private var boardMovesLocal = ArrayList<String>()
    private var movedToAR = false
    private var dialogShown = false

    // Text to speech
    private var tts: TextToSpeech? = null
    private var ttsToggle = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameBinding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(gameBinding.root)
        db = Firebase.firestore
        this.supportActionBar!!.hide()

        gameBinding.hideShowBoard.setOnClickListener{
            isBoardHidden = !isBoardHidden
            if (isBoardHidden) {
                gameBinding.hideShowBoard.background = getDrawable(R.drawable.shown)
            } else {
                gameBinding.hideShowBoard.background = getDrawable(R.drawable.hidden)
            }
            renderBoardState()
        }
        
        gameBinding.hideShowBoardBlack.setOnClickListener{
            isBoardHidden = !isBoardHidden
            renderBoardState()
        }
        
        // Get Local Player Name
        localPlayerName = intent.getStringExtra("name").toString()

        // Get identity
        identity = intent.getStringExtra("identity").toString()

        // Assign Voice Command Listener to Button
        gameBinding.voiceCommandBtn.setOnTouchListener(voiceCommandButtonTouchListener)
        gameBinding.voiceCommandBtnBlack.setOnTouchListener(voiceCommandButtonTouchListener)
        GlobalScope.launch(Dispatchers.Default) {
            speechlyClient.onSegmentChange { segment: Segment ->
                finalSegment = segment
                val transcript = segment.words.values.map{it.value}.joinToString(" ")
                GlobalScope.launch(Dispatchers.Main) {
                    gameBinding.voiceResultTextField.text = transcript
                    gameBinding.voiceResultTextFieldBlack.text = transcript
                    Log.d("DEBUG", transcript)
                }
                Log.i("cliffen current segment", transcript)
            }
        }
        tts = TextToSpeech(this, this)

        // Generate a new board
        board = Board()

        // Initialize local boardHistoryArray for local multiplayer
        boardHistoryLocal.add(board.fen)
        boardMovesLocal.add("Start")

        //Check for Online Game
        roomId = intent.getStringExtra("roomId").toString()
        if(roomId != "null" && !isOnlineGameIntialized) {
            gameBinding.onlineGameHeader.visibility = View.VISIBLE
            gameBinding.voiceCommandBtnBlack.visibility = View.GONE
            setupOnlineGame()
        }

        // Create Image button tiles in the layout
        generateChessBoardTileButtons()
        // Set the Image in the Image button based on pieces in board class
        renderBoardState()
        // Set All Current Legal Moves
        currentLegalMoves.addAll(board.legalMoves())

        // Generate a movelist
        moveList = MoveList()
    }

    private var voiceCommandButtonTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (gameBinding.voiceCommandBtn.isClickable == true || gameBinding.voiceCommandBtnBlack.isClickable == true) {
                        gameBinding.voiceResultTextField.text = ""
                        gameBinding.voiceResultTextFieldBlack.text = ""
                    }
                    speechlyClient.startContext()
                }
                MotionEvent.ACTION_UP -> {
                    try {
                        speechlyClient.stopContext()
                        GlobalScope.launch(Dispatchers.Default) {
                            delay(1500)
                            val transcript = finalSegment.words.values.map{it.value}.joinToString(" ")
                            Log.i("cliffen final segment", transcript)
                            GlobalScope.launch(Dispatchers.Main) {
                                gameBinding.voiceResultTextField.text = transcript
                                gameBinding.voiceResultTextFieldBlack.text = transcript
                                Log.d("DEBUG", transcript)
                                try {
                                    if (finalSegment.words.values.size >= 5) {
                                        movePieceWithVoiceCommand(transcript)
                                    }
                                } catch(error: Error) {
                                    Log.d("ERROR", error.toString())
                                }
                                gameBinding.voiceResultTextField.text = "Tap and hold on this side of the screen to speak"
                            }
                        }
                    } catch (exception: NoActiveStreamException) {
                        gameBinding.voiceResultTextField.text = "Please wait..."
                        gameBinding.voiceResultTextFieldBlack.text = "Please wait..."
                        gameBinding.voiceCommandBtn.isClickable = false
                        gameBinding.voiceCommandBtnBlack.isClickable = false
                        val handler = Handler()
                        handler.postDelayed({
                            speechlyClient.stopContext()
                            gameBinding.voiceResultTextField.text = "Tap and hold on this side of the screen to speak"
                            gameBinding.voiceResultTextFieldBlack.text = "Tap and hold on this side of the screen to speak"
                            gameBinding.voiceCommandBtn.isClickable = true
                            gameBinding.voiceCommandBtnBlack.isClickable = true
                        },3000)

                        Log.i("cliffen", "speechly tap exception")
                    } catch (error: Error) {
                        Log.i("cliffen", "speechly tap exception")
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
            if (isOnlineGame && identity != "owner") {
                index = boardArrayList.size - 1 - i
                for (tile in boardArrayList[index].reversed()) {
                    tile.tag = chessTiles.size
                    chessTiles.add(tile)
                    tile.setOnClickListener {
                        val tileIndex = it.tag.toString().toInt()
                        selectTileAtIndex(tileIndex)
                    }
                }
            } else {
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
    }

    private fun renderBoardState () {
        // Get the Current State of the Chess Board in an Array Sequence
        // Each Index Represents a Tile on the Board and The Item Represents the Piece Type
        val currentBoardStateArray = board.boardToArray()

        for (i in 0 until 64) {

            // Image Button to be Rendered on
            val chessTile = chessTiles[i]

            // Remove disabled view text
            if (!isBoardHidden) {
                gameBinding.disabledViewText.visibility = View.GONE

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
            } else {
                gameBinding.disabledViewText.visibility = View.VISIBLE
                if (!isOnlineGame) {
                    gameBinding.disabledViewSeparator.visibility = View.VISIBLE
                    gameBinding.disabledViewBlack.visibility = View.VISIBLE
                }
                chessTile.setImageResource(0)
                chessTile.setBackgroundColor(ContextCompat.getColor(this, R.color.chess_border))
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
            val squareSelectedIdx = Square.squareAt(tileSelectedIndex)
            val squareIdx = Square.squareAt(tileIndex)

            // Check New Move Object is a promotion
            var newMove = Move(Square.squareAt(tileSelectedIndex),Square.squareAt(tileIndex))
            if (sideToMove == Side.WHITE && newMove.to.rank == Rank.RANK_8 && board.getPiece(Square.squareAt(tileSelectedIndex)) == Piece.WHITE_PAWN) {
                val testPromotionMove = Move(Square.squareAt(tileSelectedIndex),Square.squareAt(tileIndex), Piece.WHITE_QUEEN)
                if(testPromotionMove in currentLegalMoves) {
                    openPromotionDialog(sideToMove, newMove)
                }
            } else if (sideToMove == Side.BLACK && newMove.to.rank == Rank.RANK_1 && board.getPiece(Square.squareAt(tileSelectedIndex)) == Piece.BLACK_PAWN) {
                val testPromotionMove = Move(Square.squareAt(tileSelectedIndex),Square.squareAt(tileIndex), Piece.BLACK_QUEEN)
                if(testPromotionMove in currentLegalMoves) {
                    openPromotionDialog(sideToMove, newMove)
                }
            }
            // Check if New Move is Legal

            if(newMove in currentLegalMoves) {
                saveMoveIntoMoveList(board.clone(), newMove)
                board.doMove(newMove)
                if (ttsToggle) tts!!.speak("$squareSelectedIdx to $squareIdx", TextToSpeech.QUEUE_FLUSH, null,"")
                tileSelectedIndex = -1
                if (isOnlineGame){
                    sendBoardStateOnline("$squareSelectedIdx to $squareIdx")
                } else {
                    boardHistoryLocal.add(board.fen)
                    boardMovesLocal.add("$squareSelectedIdx to $squareIdx")
                }

                // Save board state to boardHistoryLocal array if game is offline
                renderBoardState()
                afterMoveHandler()
            }
        }
    }

    private fun openPromotionDialog(side: Side, newMove: Move){
        val myDialog = Dialog(this)
        myDialog.setContentView(R.layout.pawn_promotion_popup)

        val queenBtn = myDialog.findViewById<ImageButton>(R.id.queenBtn)
        val knightBtn = myDialog.findViewById<ImageButton>(R.id.knightBtn)
        val bishopBtn = myDialog.findViewById<ImageButton>(R.id.bishopBtn)
        val rookBtn = myDialog.findViewById<ImageButton>(R.id.rookBtn)
        if(side == Side.WHITE) {
            queenBtn.setImageResource(R.drawable.w_queen_2x_ns)
            knightBtn.setImageResource(R.drawable.w_knight_2x_ns)
            bishopBtn.setImageResource(R.drawable.w_bishop_2x_ns)
            rookBtn.setImageResource(R.drawable.w_rook_2x_ns)
        } else {
            queenBtn.setImageResource(R.drawable.b_queen_2x_ns)
            knightBtn.setImageResource(R.drawable.b_knight_2x_ns)
            bishopBtn.setImageResource(R.drawable.b_bishop_2x_ns)
            rookBtn.setImageResource(R.drawable.b_rook_2x_ns)
        }
        queenBtn.setOnClickListener {
            if(side == Side.WHITE) {
                movePieceAndPromote(newMove, Piece.WHITE_QUEEN)
            } else {
                movePieceAndPromote(newMove, Piece.BLACK_QUEEN)
            }
            myDialog.dismiss()
        }
        knightBtn.setOnClickListener {
            if(side == Side.WHITE) {
                movePieceAndPromote(newMove, Piece.WHITE_KNIGHT)
            } else {
                movePieceAndPromote(newMove, Piece.BLACK_KNIGHT)
            }
            myDialog.dismiss()
        }
        bishopBtn.setOnClickListener {
            if(side == Side.WHITE) {
                movePieceAndPromote(newMove, Piece.WHITE_BISHOP)
            } else {
                movePieceAndPromote(newMove, Piece.BLACK_BISHOP)
            }
            myDialog.dismiss()
        }
        rookBtn.setOnClickListener {
            if(side == Side.WHITE) {
                movePieceAndPromote(newMove, Piece.WHITE_ROOK)
            } else {
                movePieceAndPromote(newMove, Piece.BLACK_ROOK)
            }
            myDialog.dismiss()
        }
        myDialog.show()
    }
    
    private fun movePieceAndPromote(move: Move, piece: Piece){
        val newMove = Move(move.from, move.to, piece)
        if(newMove in currentLegalMoves) {
            saveMoveIntoMoveList(board.clone(), newMove)
            board.doMove(newMove)
            val squareSelectedIdx = newMove.from
            val squareIdx = newMove.to
            if (ttsToggle) tts!!.speak("$squareSelectedIdx to $squareIdx, promoted to ${piece.toString()}", TextToSpeech.QUEUE_FLUSH, null,"")
            // Save board state to boardHistoryLocal array if game is offline
            renderBoardState()
            tileSelectedIndex = -1
            if (isOnlineGame){
                sendBoardStateOnline("$squareSelectedIdx to $squareIdx")
            } else {
                boardHistoryLocal.add(board.fen)
                boardMovesLocal.add("$squareSelectedIdx to $squareIdx")
            }
            afterMoveHandler()

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
            currentLegalMoves.clear()
            currentLegalMoves.addAll(board.legalMoves())

            // Check New Move Object is a promotion
            if (sideToMove == Side.WHITE && newMove.to.rank == Rank.RANK_8 && board.getPiece(from) == Piece.WHITE_PAWN) {
                val testPromotionMove = Move(from, to, Piece.WHITE_QUEEN)
                if(testPromotionMove in currentLegalMoves) {
                    openPromotionDialog(sideToMove, newMove)
                    return
                }
            } else if (sideToMove == Side.BLACK && newMove.to.rank == Rank.RANK_1 && board.getPiece(from) == Piece.BLACK_PAWN) {
                val testPromotionMove = Move(from, to, Piece.BLACK_QUEEN)
                if(testPromotionMove in currentLegalMoves) {
                    openPromotionDialog(sideToMove, newMove)
                    return
                }
            }

            if(newMove in currentLegalMoves) {
                saveMoveIntoMoveList(board.clone(), newMove)
                board.doMove(newMove)
                if (ttsToggle) tts!!.speak("$from to $to", TextToSpeech.QUEUE_FLUSH, null,"")
                renderBoardState()
                if (isOnlineGame){
                    sendBoardStateOnline("$from to $to")
                } else {
                    boardHistoryLocal.add(board.fen)
                    boardMovesLocal.add("$from to $to")
                }
                gameBinding.voiceResultTextField.text = "Tap and hold on this side of the screen to speak"
                afterMoveHandler()

            } else {
                throw Error("Invalid Command")
            }
        } catch (error: Error) {
            Toast.makeText(this, "Invalid Command. Please Try Again", Toast.LENGTH_SHORT).show()
            if (ttsToggle) tts!!.speak("Invalid Command", TextToSpeech.QUEUE_FLUSH, null,"")

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
             //remove firestore snapshot and success listener
            if (isOnlineGame) {
                snapshotListener.remove()
                deleteRoomDocument()
                roomLeft = true
            }
            val it = Intent()
            setResult(RESULT_OK, it)
            finish()
        }
        if (board.isMated) {
            moveSanList.clear()
            if (!dialogShown) {
                myDialog.show()
                dialogShown = true
            }
            val winner = board.sideToMove.flip().toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)
            val result = "$winner Wins"
            if (!isOnlineGame) {
                saveBoardHistory(winner)
            }
            myDialog.findViewById<TextView>(R.id.resultText).text = result
            if(board.sideToMove == Side.BLACK){
                myDialog.findViewById<ShapeableImageView>(R.id.whiteAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
            } else {
                myDialog.findViewById<ShapeableImageView>(R.id.blackAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
            }
        } else if (board.isDraw) {
            if (board.isRepetition) {
                // TODO clear movelist
                moveSanList.clear()
                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
                myDialog.show()
            } else if (board.isInsufficientMaterial) {
                // TODO clear movelist
                moveSanList.clear()
                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
                myDialog.show()
            } else if (board.halfMoveCounter >= 100) {
                // TODO clear movelist
                moveSanList.clear()
                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
                myDialog.show()
            }
            else if (board.isStaleMate){
                // TODO clear movelist
                moveSanList.clear()
                myDialog.findViewById<TextView>(R.id.resultText).setText("Stale Mate")
                myDialog.show()
            }
            if (!isOnlineGame) {
                saveBoardHistory("")
            }
            if (!dialogShown) {
                myDialog.show()
                dialogShown = true
            }
        }
    }

    private fun deleteRoomDocument() {
        val TAG = "cliffen"
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        val roomClosed = document.data!!["roomClosed"]

                        if (document.get("roomClosed") !== null) {
                            // delete room from firestore
                            db.collection("rooms").document(roomId)
                                    .delete()
                                    .addOnSuccessListener { Log.d("cliffen", "DocumentSnapshot successfully deleted!") }
                                    .addOnFailureListener { e -> Log.w("cliffen", "Error deleting document", e) }
                        } else {
                            val roomClosed = hashMapOf("roomClosed" to 1)
                            roomRef.set(roomClosed, SetOptions.merge())
                        }

                    } else {
                        Log.d(TAG, "Failed to delete document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
    }

    private fun saveBoardHistory(winner: String) {
        Log.i("cliffen", "saving board history from game activity")

        val TAG = "cliffen"
        val roomRef = db.collection("rooms").document(roomId)

        try {
                val fileOutputStream: FileOutputStream = openFileOutput("${localPlayerName}.txt", Context.MODE_APPEND)
                val outputWriter = OutputStreamWriter(fileOutputStream)

                if (isOnlineGame && !boardSaved) {
                    roomRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                                    boardHistory = document.data!!["boardHistory"]!! as ArrayList<String>
                                    boardMoves = document.data!!["boardMoves"]!! as ArrayList<String>
                                    if (identity == "owner") {
                                        outputWriter.write("$localPlayerName vs $opponent  $boardHistory  $boardMoves  $winner  ${LocalDate.now()}" + "\n")
                                    } else {
                                        outputWriter.write("$opponent vs $localPlayerName  $boardHistory  $boardMoves  $winner  ${LocalDate.now()}" + "\n")
                                    }
                                    boardSaved = true
                                    outputWriter.close()

                                } else {
                                    if (ttsToggle) tts!!.speak("Failed to add match to history", TextToSpeech.QUEUE_FLUSH, null,"")
                                    Toast.makeText(this, "Failed to add match to history!", LENGTH_SHORT).show()
                                    Log.d(TAG, "No such document")
                                }
                            }
                            .addOnFailureListener { exception ->
                                if (ttsToggle) tts!!.speak("Failed to add match to history", TextToSpeech.QUEUE_FLUSH, null,"")
                                Toast.makeText(this, "Failed to add match to history!", LENGTH_SHORT).show()
                                Log.d(TAG, "get failed with ", exception)
                            }
                } else {
                    outputWriter.write("Local game  $boardHistoryLocal  $boardMovesLocal  $winner  ${LocalDate.now()}" + "\n")
                    boardSaved = true
                    outputWriter.close()
                }

            }

        catch (e: Exception) {
            if (ttsToggle) tts!!.speak("Failed to add match to history", TextToSpeech.QUEUE_FLUSH, null,"")
            Toast.makeText(this, "Failed to add match to history!", LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun setupOnlineGame() {

        isOnlineGame = true
        val roomRef = db.collection("rooms").document(roomId)

        // Initialize turn variable
        var turnData = hashMapOf("currentTurn" to board.sideToMove.toString())
        roomRef.set(turnData, SetOptions.merge())

        // set board history if owner
        if (identity == "owner") {
            val boardHistory = ArrayList<String>()
            boardHistory.add(board.fen)
            boardMoves.add("Start")
            val historyData = hashMapOf(
                    "currentTurn" to board.sideToMove.toString(),
                    "boardHistory" to boardHistory,
                    "boardMoves" to boardMoves)
            roomRef.set(historyData, SetOptions.merge())
        }


        // Setup Headers
        roomRef.get().addOnSuccessListener { document ->
            if (document != null) {
                snapshotListener = roomRef.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.i("cliffen", "debug snapshot: ${snapshot.data}")
                        var boardMoves = ArrayList<String>()
                        if (snapshot.data!!.get("boardMoves") != null) {
                            boardMoves = snapshot.data?.get("boardMoves") as ArrayList<String>
                        }

                        Log.i("cliffen", "board moves: $boardMoves")

                        // Disconnect players from game when someone disconnects
                        if (snapshot.get("roomClosed") !== null && !board.isMated) {
                            val myDialog = Dialog(this)
                            myDialog.setContentView(R.layout.game_finish_popup)
                            myDialog.setCanceledOnTouchOutside(false)
                            myDialog.setCancelable(false)
                            myDialog.findViewById<Button>(R.id.returnBtn).setOnClickListener{
                                //remove firestore snapshot and success listener
                                if (!roomLeft) {
                                    snapshotListener.remove()
                                    deleteRoomDocument()
                                    roomLeft = true
                                }
                                val it = Intent()
                                setResult(RESULT_OK, it)
                                finish()
                            }
                            myDialog.findViewById<TextView>(R.id.resultText).text = ("Game disconnected")
                            myDialog.show()
                        }

                        // On Board State Change
                        val onlineBoardState = snapshot.data!!["boardState"].toString()

                        if (onlineBoardState != "null" && board.fen != onlineBoardState) {
                            if ( snapshot.get("boardMoves") !== null && document.data!!["boardMoves"] !== null) {
                                val testBoard = Board()
                                testBoard.loadFromFen(onlineBoardState)
                                Log.i("cliffen", "current board fen: ${board.fen}")
                                Log.i("cliffen", "online board fen: ${testBoard.fen}")
                                val onlineMove = boardMoves[boardMoves.size -1]
                                Log.i("cliffen", "ONILNE DEBUG MOVE: $onlineMove")
                                if (onlineMove.contains("to")) {
                                    if (ttsToggle) tts!!.speak(onlineMove, TextToSpeech.QUEUE_FLUSH, null,"")
                                    Log.i("cliffen", "move is $onlineMove")
                                    val from = Square.fromValue(onlineMove.split(" to ")[0])
                                    val to = Square.fromValue(onlineMove.split(" to ")[1])
                                    val newMove = Move(from, to)
                                    Log.i("cliffen", "newMove: $newMove")
                                    saveMoveIntoMoveList(board.clone(), newMove)
                                }
                            }

                            board.loadFromFen(onlineBoardState)
                            renderBoardState()
                            afterMoveHandler()
                            Log.i("cliffen", board.sideToMove.toString())
                            Log.i("cliffen", localPlayerColor.toString())

                            currentLegalMoves.clear()
                            currentLegalMoves.addAll(board.legalMoves())
                        }

                        if (board.sideToMove == localPlayerColor) {
                            gameBinding.onlineGameTurnText.text = "Your (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                        } else {
                            Log.i("cliffen", "change header to opponent")
                            gameBinding.onlineGameTurnText.text = "${onlinePlayerName}'s (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                        }

                        // save match history if mated
                        if (!boardSaved) {
                            if (board.isMated) {
                                val winner = board.sideToMove.flip().toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)
                                saveBoardHistory(winner)
                            } else if (board.isDraw) {
                                if (board.isRepetition) {
                                    saveBoardHistory("")
                                } else if (board.isInsufficientMaterial) {
                                    saveBoardHistory("")
                                } else if (board.halfMoveCounter >= 100) {
                                    saveBoardHistory("")
                                }
                                else if (board.isStaleMate){
                                    saveBoardHistory("")
                                }
                            }
                        }
                    }
                }


                val owner = document.data?.getValue("owner").toString()
                val player = document.data?.getValue("player").toString()
                if (identity == "owner") {
                    opponent = player
                    localPlayerColor  = Side.WHITE
                    onlinePlayerName = player
                    gameBinding.onlineGameTitle.text = "Currently playing with $onlinePlayerName"
                    gameBinding.onlineGameTurnText.text = "Your (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                } else {
                    opponent = owner
                    localPlayerColor  = Side.BLACK
                    onlinePlayerName = owner
                    gameBinding.onlineGameTitle.text = "Currently playing with $onlinePlayerName"
                    gameBinding.onlineGameTurnText.text = "${onlinePlayerName}'s (${board.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                }

            }
        }

        isOnlineGameIntialized = true

    }

    private fun sendBoardStateOnline(move: String) {
        val TAG = "cliffen"
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    boardHistory = document.data!!["boardHistory"] as ArrayList<String>
                    Log.i("cliffen before", boardHistory.toString())
                    boardHistory.add(board.fen)
                    Log.i("cliffen after", boardHistory.toString())

                    // Get board moves
                    boardMoves = document.data!!["boardMoves"] as ArrayList<String>
                    boardMoves.add(move)
                    Log.i("cliffen", "setting online board move as: $boardMoves")
                    val boardData = hashMapOf(
                        "boardState" to board.fen,
                        "boardHistory" to boardHistory,
                        "boardMoves" to boardMoves)
                    roomRef.set(boardData, SetOptions.merge())
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    override fun onStop() {
        super.onStop()
        if (isOnlineGame) {
            snapshotListener.remove()
        }
        Log.i("cliffen", "game on stop launched")
        if (isOnlineGame && !roomLeft && !movedToAR) {
            deleteRoomDocument()
        }
    }


    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.UK)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }

    }

    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    fun flipView(view: View) {
        val flipper = findViewById<ViewFlipper>(R.id.viewFlipper)
        flipper.showNext()
    }

    fun flipViewBlack(view: View) {
        val flipper = findViewById<ViewFlipper>(R.id.viewFlipperBlack)
        flipper.showNext()
    }

    fun toggleTTS(view: View) {
        ttsToggle = !ttsToggle
        if (ttsToggle) {
            gameBinding.sound.background = getDrawable(R.drawable.sound_off)
        } else {
            gameBinding.sound.background = getDrawable(R.drawable.sound_on)
            tts?.stop()
        }
    }

    private fun saveMoveIntoMoveList(previousBoard:Board, newMove: Move) {
//        Log.i("cliffen", "move into move list called on: $previousBoard")
        val moveListLocal = MoveListLocal()
        val san = moveListLocal.encodeSan(previousBoard, newMove)

        if (firstMove) {
            gameBinding.timer.base = SystemClock.elapsedRealtime()
            gameBinding.timerBlack.base = SystemClock.elapsedRealtime()
            gameBinding.timer.start()
            gameBinding.timerBlack.start()
            firstMove = false
        }

        if (moveSanList.size == 6) {
            moveSanList.clear()
            moveSanList.add(san)
        } else {
            moveSanList.add(san)
        }
        val move1 = findViewById<TextView>(R.id.move1)
        val move2 = findViewById<TextView>(R.id.move2)
        val move3 = findViewById<TextView>(R.id.move3)
        val move4 = findViewById<TextView>(R.id.move4)
        val move5 = findViewById<TextView>(R.id.move5)
        val move6 = findViewById<TextView>(R.id.move6)
        val movesWhiteCol = mutableListOf(move1, move3, move5)
        val movesBlackCol = mutableListOf(move2, move4, move6)
        val move1Black = findViewById<TextView>(R.id.move1Black)
        val move2Black = findViewById<TextView>(R.id.move2Black)
        val move3Black = findViewById<TextView>(R.id.move3Black)
        val move4Black = findViewById<TextView>(R.id.move4Black)
        val move5Black = findViewById<TextView>(R.id.move5Black)
        val move6Black = findViewById<TextView>(R.id.move6Black)
        val movesBlackWhiteCol = mutableListOf(move1Black, move3Black, move5Black)
        val movesBlackBlackCol = mutableListOf(move2Black, move4Black, move6Black)
        var flag = true
        var whiteMoves = ArrayList<String>()
        var blackMoves = ArrayList<String>()
        for (move in 0 until moveSanList.size) {
            if (flag) {
                // add to white
                whiteMoves.add(moveSanList[move])
                flag = false
            } else {
                // add to black
                blackMoves.add(moveSanList[move])
                flag = true
            }
        }
        // change white moves text
        for (text in 0 until whiteMoves.size) {
            movesWhiteCol[whiteMoves.size - text - 1].text = whiteMoves[text]
            movesBlackWhiteCol[whiteMoves.size - text - 1].text = whiteMoves[text]
        }
        if (moveSanList.size % 2 != 0) {
            // white > black
            for (text in 0 until blackMoves.size) {
                movesBlackCol[blackMoves.size - text].text = blackMoves[text]
                movesBlackCol[0].text = ""
                movesBlackBlackCol[blackMoves.size - text].text = blackMoves[text]
                movesBlackBlackCol[0].text = ""
            }
        } else {
            // white == black
            for (text in 0 until blackMoves.size) {
                movesBlackCol[blackMoves.size - text - 1].text = blackMoves[text]
                movesBlackBlackCol[blackMoves.size - text - 1].text = blackMoves[text]
            }
        }
        if (moveSanList.size == 1) {
            movesBlackCol[2].text = movesBlackCol[1].text
            movesBlackCol[1].text = movesBlackCol[0].text
            movesBlackCol[0].text = ""
            movesBlackBlackCol[2].text = movesBlackBlackCol[1].text
            movesBlackBlackCol[1].text = movesBlackBlackCol[0].text
            movesBlackBlackCol[0].text = ""
        }
    }

    public fun NavigateToAR(view:View) {
        movedToAR = true
        val it = Intent(this, ArActivity::class.java)
        it.putExtra("roomId", roomId)
        it.putExtra("name", localPlayerName)
        it.putExtra("identity", identity)
        it.putExtra("board", board.fen)
        startActivityForResult(it, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (resultCode == 1500) {
                movedToAR = false
                Log.i("cliffen", "returned from AR")
                val boardFen = data!!.getStringExtra("board")
                Log.i("cliffen", "fen data: $boardFen")
                board.loadFromFen(data!!.getStringExtra("board"))
                renderBoardState()
            } else {
                movedToAR = true
                boardSaved = data!!.getBooleanExtra("arSavedAlready", false)
                val it = Intent()
                setResult(RESULT_OK, it)
                finish()
            }

        }

    }
}