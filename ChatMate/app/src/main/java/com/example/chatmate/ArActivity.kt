package com.example.chatmate

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chatmate.databinding.ActivityArBinding
import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.google.android.gms.tasks.Task
import com.google.android.material.imageview.ShapeableImageView
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
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
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.Exception
import java.lang.Math.round
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor


/**
 * This is augmented reality chess built using Google's HelloSceneform as a base.
 */
class ArActivity : AppCompatActivity(), TextToSpeech.OnInitListener  {
    // debugging variable
    private val TAG = "cliffen"

    private lateinit var binding: ActivityArBinding
    private var arFragment: ArFragment? = null

    // speechly variables
    private lateinit var finalSegment: Segment
    private val speechlyClient: Client = Client.fromActivity(activity = this, appId = UUID.fromString("8a313e01-b0f3-4e6f-94a9-67cd65433135"))

    // tts
    private var tts: TextToSpeech? = null

    // white pieces renderable
    private var whitePawnRenderable: ModelRenderable? = null
    private var whiteBishopRenderable: ModelRenderable? = null
    private var whiteQueenRenderable: ModelRenderable? = null
    private var whiteKingRenderable: ModelRenderable? = null
    private var whiteRookRenderable: ModelRenderable? = null
    private var whiteKnightRenderable: ModelRenderable? = null
    
    // black pieces renderable
    private var blackPawnRenderable: ModelRenderable? = null
    private var blackBishopRenderable: ModelRenderable? = null
    private var blackQueenRenderable: ModelRenderable? = null
    private var blackKingRenderable: ModelRenderable? = null
    private var blackRookRenderable: ModelRenderable? = null
    private var blackKnightRenderable: ModelRenderable? = null

    // board renderable
    private var boardRenderable: ModelRenderable? = null
    private var tileRenderable: ModelRenderable? = null
    private var selectedRenderable: ModelRenderable? = null

    // node list
    private var nodeList = ArrayList<Node>()
    private lateinit var anchorNode: AnchorNode
    
    // board node
    private var board: Node? = null

    // virtual game variables
    private lateinit var boardHistory: ArrayList<String>
    private var boardHistoryLocal = ArrayList<String>()
    private lateinit var virtualBoard: Board
    private val currentLegalMoves = ArrayList<Move>()
    private var isOnlineGame = false
    private var tileSelectedIndex = -1
    private lateinit var roomId: String
    private lateinit var identity: String
    private lateinit var localPlayerColor:Side
    private lateinit var onlinePlayerName: String
    private lateinit var successListener: Task<DocumentSnapshot>
    private lateinit var snapshotListener: ListenerRegistration
    private var roomLeft = false
    private var boardSaved = false
    private var isOnlineGameIntialized = false
    private lateinit var db: FirebaseFirestore
    private lateinit var localPlayerName: String
    private lateinit var opponent: String
    private var dialogShown = false

    // boolean to disable plane tap listener
    private var boardRendered = false

    // scaling and movement vectors
    private val halfBoard = 0.02937f
    private val pieceScaleVector = Vector3(0.025f, 0.025f, 0.025f)
    private val boardScaleVector = Vector3(1.7f, 1.7f, 1.7f)


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

    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        binding = ActivityArBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.supportActionBar!!.hide()
        db = Firebase.firestore
        arFragment = getSupportFragmentManager().findFragmentById(R.id.ux_fragment) as ArFragment

        // Assign Voice Command Listener to Button
        binding.voiceCommandBtn.setOnTouchListener(voiceCommandButtonTouchListener)
        GlobalScope.launch(Dispatchers.Default) {
            speechlyClient.onSegmentChange { segment: Segment ->
                finalSegment = segment
                val transcript = segment.words.values.map{it.value}.joinToString(" ")
                Log.i("cliffen current segment", transcript)
            }
        }

        //tts
        tts = TextToSpeech(this, this)

        // Get intent values
        localPlayerName = intent.getStringExtra("name").toString()
        identity = intent.getStringExtra("identity").toString()
        roomId = intent.getStringExtra("name").toString()
        virtualBoard = Board()
        virtualBoard.loadFromFen(intent.getStringExtra("board").toString())

        // add local board history
        boardHistoryLocal.add(virtualBoard.fen)

        //Check for Online Game
        roomId = intent.getStringExtra("roomId").toString()
        if(roomId != "null" && !isOnlineGameIntialized) {
            setupOnlineGame()
        }

        // Set All Current Legal Moves
        currentLegalMoves.addAll(virtualBoard.legalMoves())

        // WHITE
        // Build pawn renderable
            ModelRenderable.builder()
                    .setSource(this, R.raw.whitepawn)
                    .build()
                    .thenAccept { renderable -> whitePawnRenderable = renderable }
                    .exceptionally { throwable ->
                        val toast: Toast =
                                Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        toast.show()
                        null
                    }

        // Build rook renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.whiterook)
                .build()
                .thenAccept { renderable -> whiteRookRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build knight renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.whiteknight)
                .build()
                .thenAccept { renderable -> whiteKnightRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build bishop renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.whitebishop)
                .build()
                .thenAccept { renderable -> whiteBishopRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build queen renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.whitequeen)
                .build()
                .thenAccept { renderable -> whiteQueenRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build king renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.whiteking)
                .build()
                .thenAccept { renderable -> whiteKingRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // BLACK
        // Build pawn renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.blackpawn)
                .build()
                .thenAccept { renderable -> blackPawnRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build rook renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.blackrook)
                .build()
                .thenAccept { renderable -> blackRookRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build knight renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.blackknight)
                .build()
                .thenAccept { renderable -> blackKnightRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build bishop renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.blackbishop)
                .build()
                .thenAccept { renderable -> blackBishopRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build queen renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.blackqueen)
                .build()
                .thenAccept { renderable -> blackQueenRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build king renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.blackking)
                .build()
                .thenAccept { renderable -> blackKingRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }
        
        // Build board renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.chessboardsample)
                .build()
                .thenAccept { renderable -> boardRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build tile renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.tile)
                .build()
                .thenAccept { renderable -> tileRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        // Build selected renderable
        ModelRenderable.builder()
                .setSource(this, R.raw.selected)
                .build()
                .thenAccept { renderable -> selectedRenderable = renderable }
                .exceptionally { throwable ->
                    val toast: Toast =
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (!boardRendered) {
                if (boardRenderable == null) return@setOnTapArPlaneListener

                // Create the Anchor.
                val anchor = hitResult.createAnchor()
                anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arFragment!!.arSceneView.scene)
                if (board != null) return@setOnTapArPlaneListener

                // for online multiplayer
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    // render on board according to current board state
                    board = Node()
                    board!!.setParent(anchorNode)
                    board!!.localScale = boardScaleVector
                    board!!.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                    board!!.renderable = boardRenderable
                    renderBoardState()
                } else {
                    board = Node()
                    board!!.setParent(anchorNode)
                    board!!.localScale = boardScaleVector
                    board!!.renderable = boardRenderable
                    renderBoardState()
                }
                boardRendered = true
            }
        }
    }

    companion object {
        private val TAG = ArActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0

        /**
         * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
         * on this device.
         *
         *
         * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
         *
         *
         * Finishes the activity if Sceneform can not run
         */
        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
                Log.e(TAG, "Sceneform requires Android N or later")
                Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG)
                    .show()
                activity.finish()
                return false
            }
            val openGlVersionString =
                (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(
                        activity,
                        "Sceneform requires OpenGL ES 3.0 or later",
                        Toast.LENGTH_LONG
                )
                    .show()
                activity.finish()
                return false
            }
            return true
        }
    }

    private var voiceCommandButtonTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.voiceResultTextField.text = ""
                    speechlyClient.startContext()
                }
                MotionEvent.ACTION_UP -> {
                    speechlyClient.stopContext()
                    GlobalScope.launch(Dispatchers.Default) {
                        delay(1500)
                        val transcript = finalSegment.words.values.map{it.value}.joinToString(" ")
                        Log.i("cliffen final segment", transcript)
                        GlobalScope.launch(Dispatchers.Main) {
                            binding.voiceResultTextField.text = transcript
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

    private fun movePieceWithVoiceCommand(command: String){
        val sideToMove = virtualBoard.sideToMove
//        if (isOnlineGame && localPlayerColor != sideToMove) {
//            return
//        }
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
                if (isOnlineGame){
                    sendvirtualBoardStateOnline()
                }
                virtualBoard.doMove(newMove)
//                movePiece(from.toString(), to.toString())
                
                tts!!.speak("$from to $to", TextToSpeech.QUEUE_FLUSH, null,"")
                renderBoardState()
                currentLegalMoves.clear()
                currentLegalMoves.addAll(virtualBoard.legalMoves())
                binding.voiceResultTextField.text = "Tap and hold to speak"
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
        if (virtualBoard.isMated) {
            if (!dialogShown) {
                myDialog.show()
                dialogShown = true
            }
            val winner = virtualBoard.sideToMove.flip().toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)
            val result = "$winner Wins"
            if (!isOnlineGame) {
                saveBoardHistory(winner)
            }
            myDialog.findViewById<TextView>(R.id.resultText).text = result
            if(virtualBoard.sideToMove == Side.BLACK){
                myDialog.findViewById<ShapeableImageView>(R.id.whiteAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
            } else {
                myDialog.findViewById<ShapeableImageView>(R.id.blackAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
            }
        } else if (virtualBoard.isDraw) {
            var result = "Match Draw"

            if (virtualBoard.isStaleMate) {
                result = "Stale Mate"
            }
            myDialog.findViewById<TextView>(R.id.resultText).text = result
            if (!isOnlineGame) {
                saveBoardHistory("")
            }
            if (!dialogShown) {
                myDialog.show()
                dialogShown = true
            }
        }
    }

    private fun saveBoardHistory(winner: String) {
//
//        val TAG = "cliffen"
//        val roomRef = db.collection("rooms").document(roomId)
//
//        try {
//            val fileOutputStream: FileOutputStream = openFileOutput("${localPlayerName}.txt", Context.MODE_APPEND)
//            val outputWriter = OutputStreamWriter(fileOutputStream)
//
//            if (isOnlineGame) {
//                roomRef.get()
//                        .addOnSuccessListener { document ->
//                            if (document != null && document.exists()) {
//                                Log.d(TAG, "DocumentSnapshot data: ${document.data}")
//                                boardHistory = document.data!!["boardHistory"]!! as ArrayList<String>
//                                if (identity == "owner") {
//                                    outputWriter.write("$localPlayerName vs $opponent  $boardHistory  $winner " + "\n")
//                                } else {
//                                    outputWriter.write("$opponent vs $localPlayerName  $boardHistory  $winner " + "\n")
//                                }
//                                boardSaved = true
//                                outputWriter.close()
//
//                            } else {
//                                Toast.makeText(this, "Failed to add match to history!", Toast.LENGTH_SHORT).show()
//                                Log.d(TAG, "No such document")
//                            }
//                        }
//                        .addOnFailureListener { exception ->
//                            Toast.makeText(this, "Failed to add match to history!", Toast.LENGTH_SHORT).show()
//                            Log.d(TAG, "get failed with ", exception)
//                        }
//            } else {
//                outputWriter.write("Local game  $boardHistoryLocal  $winner " + "\n")
//                boardSaved = true
//                outputWriter.close()
//            }
//
//        }
//
//        catch (e: Exception) {
//            Toast.makeText(this, "Failed to add match to history!", Toast.LENGTH_SHORT).show()
//            e.printStackTrace()
//        }
    }

    private fun setupOnlineGame() {
        isOnlineGame = true
        val roomRef = db.collection("rooms").document(roomId)

        // Initialize turn variable
        var turnData = hashMapOf("currentTurn" to virtualBoard.sideToMove.toString())
        roomRef.set(turnData, SetOptions.merge())

        // set board history if owner
        if (identity == "owner") {
            val boardHistory = ArrayList<String>()
            boardHistory.add(virtualBoard.fen)
            var historyData = HashMap<String,ArrayList<String>>()
            historyData["boardHistory"] = boardHistory
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
                        // On Board State Change
                        val onlineBoardState = snapshot.data!!["boardState"].toString()


                        if (onlineBoardState !== "null" && virtualBoard.fen !== onlineBoardState) {
                            virtualBoard.loadFromFen(onlineBoardState)
                            if (boardRendered) {
                                renderBoardState()
                            }
                            afterMoveHandler()
                            if (virtualBoard.sideToMove == localPlayerColor) {
                                binding.onlineGameTurnText.text = "Your (${virtualBoard.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                            } else {
                                binding.onlineGameTurnText.text = "${onlinePlayerName}'s (${virtualBoard.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                            }
                            currentLegalMoves.clear()
                            currentLegalMoves.addAll(virtualBoard.legalMoves())
                        }

                        // Disconnect players from game when someone disconnects
                        if (snapshot.get("roomClosed") !== null && !virtualBoard.isMated) {
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
                                setResult(1001, it)
                                finish()
                            }
                            myDialog.findViewById<TextView>(R.id.resultText).text = ("Game disconnected")
                            myDialog.show()
                            dialogShown = true
                        }

                        // save match history if mated
                        if (!boardSaved) {
                            if (virtualBoard.isMated) {
                                val winner = virtualBoard.sideToMove.flip().toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)
                                saveBoardHistory(winner)
                            } else if (virtualBoard.isDraw) {
                                if (virtualBoard.isRepetition) {
                                    saveBoardHistory("")
                                } else if (virtualBoard.isInsufficientMaterial) {
                                    saveBoardHistory("")
                                } else if (virtualBoard.halfMoveCounter >= 100) {
                                    saveBoardHistory("")
                                }
                                else if (virtualBoard.isStaleMate){
                                    saveBoardHistory("")
                                }
                            }
                        }
                    }
                }

                val owner = document.data?.getValue("owner").toString()
                val player = document.data?.getValue("player").toString()
                if (owner == localPlayerName) {
                    opponent = player
                    localPlayerColor  = Side.WHITE
                    onlinePlayerName = player
                    binding.onlineGameTurnText.text = "Your (${virtualBoard.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                } else {
                    opponent = owner
                    localPlayerColor  = Side.BLACK
                    onlinePlayerName = owner
                    binding.onlineGameTurnText.text = "${onlinePlayerName}'s (${virtualBoard.sideToMove.toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)}) Turn"
                }
            }
        }

        isOnlineGameIntialized = true
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

    private fun renderBoardState () {
        // Get the Current State of the Chess Board in an Array Sequence
        // Each Index Represents a Tile on the virtualBoard and The Item Represents the Piece Type
        val currentvirtualBoardStateArray = virtualBoard.boardToArray()

        // remove all nodes on board
        for (node: Node in nodeList) {
            node.renderable = null
            node.setParent(null)
        }

        // reset nodeList
        nodeList = ArrayList<Node>()

        for (i in 0 until 64) {
            // Render node Base on Piece Value
            when (currentvirtualBoardStateArray[i].value()) {
                "WHITE_ROOK" -> renderNode("WHITE_ROOK", i);
                "WHITE_KNIGHT" -> renderNode("WHITE_KNIGHT", i)
                "WHITE_BISHOP" -> renderNode("WHITE_BISHOP", i)
                "WHITE_QUEEN" -> renderNode("WHITE_QUEEN", i)
                "WHITE_KING" -> renderNode("WHITE_KING", i)
                "WHITE_PAWN" -> renderNode("WHITE_PAWN", i)
                "BLACK_PAWN" -> renderNode("BLACK_PAWN", i)
                "BLACK_ROOK" -> renderNode("BLACK_ROOK", i)
                "BLACK_KNIGHT" -> renderNode("BLACK_KNIGHT", i)
                "BLACK_BISHOP" -> renderNode("BLACK_BISHOP", i)
                "BLACK_QUEEN" -> renderNode("BLACK_QUEEN", i)
                "BLACK_KING" -> renderNode("BLACK_KING", i)
                else -> renderNode("None", i)
            }
        }
    }

    // translate position to vector
    private fun convertToVector(position:String): Vector3 {
        var letterList = arrayListOf<Char>('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')
        val letter = letterList.indexOf(position[0])
        val number = position[1].toString().toInt()
        var x = 0f
        val y = 0.020f
        var z = 0f
        if (letter >= 4) {
            // x is positive
            x = halfBoard + (2 * halfBoard * (letter - 4))
        } else {
            // x is negative
            x = -(halfBoard + (2 * halfBoard * (3 - letter)))
        }

        if (number >= 5) {
            // z is positive
            z = -(halfBoard + (2 * halfBoard * (number - 5)))
        } else {
            // z is negative
            z = halfBoard + (2 * halfBoard * (4 - number))
        }
        return Vector3(x,y,z)
    }

    // mirror Vector positioning for black side
    private fun mirrorVector(vector: Vector3): Vector3 {
        return Vector3(-vector.x, vector.y, -vector.z)
    }

    private fun getLocalPosition (index: Int): Vector3 {
        val position = getPositionFromIndex(index)
        return convertToVector(position)
    }

    private fun vectorToIndex (vector: Vector3): Int {
        // transform vector to index
        val x = vector.x
        val z = vector.z
        var result = 0

        if (x < 0) {
            result += 4 - round( (abs(x) + halfBoard) / (2*halfBoard) ).toInt()
        } else {
            result += 4 + round( (x + halfBoard) / (2*halfBoard) ).toInt() - 1
        }
        if (z < 0) {
            result += (3 + round( (abs(z) + halfBoard) / (2*halfBoard) ).toInt()) * 8
        } else {
            result += (4 - round( (z + halfBoard) / (2*halfBoard) ).toInt()) * 8
        }

        return result
    }

    private fun renderNode(piece:String, index: Int) {
        when (piece) {
            "None" -> return
            "WHITE_ROOK" -> {
                val whiteRook = Node()
                whiteRook!!.setParent(anchorNode)
                whiteRook!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    whiteRook!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    whiteRook!!.localPosition = getLocalPosition(index)
                }
                whiteRook!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                whiteRook!!.renderable = whiteRookRenderable
                nodeList.add(whiteRook)
            }
            "WHITE_KNIGHT" -> {
                val whiteKnight = Node()
                whiteKnight!!.setParent(anchorNode)
                whiteKnight!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    whiteKnight!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    whiteKnight.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                    whiteKnight!!.localPosition = getLocalPosition(index)
                }
                whiteKnight!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                whiteKnight!!.renderable = whiteKnightRenderable
                nodeList.add(whiteKnight)
            }
            "WHITE_BISHOP" -> {
                val whiteBishop = Node()
                whiteBishop!!.setParent(anchorNode)
                whiteBishop!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    whiteBishop!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    whiteBishop.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                    whiteBishop!!.localPosition = getLocalPosition(index)
                }
                whiteBishop!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                whiteBishop!!.renderable = whiteBishopRenderable
                nodeList.add(whiteBishop)
            }
            "WHITE_QUEEN" -> {
                val whiteQueen = Node()
                whiteQueen!!.setParent(anchorNode)
                whiteQueen!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    whiteQueen!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    whiteQueen!!.localPosition = getLocalPosition(index)
                }
                whiteQueen!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                whiteQueen!!.renderable = whiteQueenRenderable
                nodeList.add(whiteQueen)
            }
            "WHITE_KING" -> {
                val whiteKing = Node()
                whiteKing!!.setParent(anchorNode)
                whiteKing!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    whiteKing!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    whiteKing!!.localPosition = getLocalPosition(index)
                }
                whiteKing!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                whiteKing!!.renderable = whiteKingRenderable
                nodeList.add(whiteKing)
            }
            "WHITE_PAWN" -> {
                val whitePawn = Node()
                whitePawn!!.setParent(anchorNode)
                whitePawn!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    whitePawn!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    whitePawn!!.localPosition = getLocalPosition(index)
                }
                whitePawn!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                whitePawn!!.renderable = whitePawnRenderable
                nodeList.add(whitePawn)
            }
            "BLACK_PAWN" -> {
                val blackPawn = Node()
                blackPawn!!.setParent(anchorNode)
                blackPawn!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    blackPawn!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    blackPawn!!.localPosition = getLocalPosition(index)
                }
                blackPawn!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                blackPawn!!.renderable = blackPawnRenderable
                nodeList.add(blackPawn)
            }
            "BLACK_ROOK" -> {
                val blackRook = Node()
                blackRook!!.setParent(anchorNode)
                blackRook!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    blackRook!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    blackRook!!.localPosition = getLocalPosition(index)
                }
                blackRook!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                blackRook!!.renderable = blackRookRenderable
                nodeList.add(blackRook)
            }
            "BLACK_KNIGHT" -> {
                val blackKnight = Node()
                blackKnight!!.setParent(anchorNode)
                blackKnight!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    blackKnight.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                    blackKnight!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    blackKnight!!.localPosition = getLocalPosition(index)
                }
                blackKnight!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                blackKnight!!.renderable = blackKnightRenderable
                nodeList.add(blackKnight)
            }
            "BLACK_BISHOP" -> {
                val blackBishop = Node()
                blackBishop!!.setParent(anchorNode)
                blackBishop!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    blackBishop.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                    blackBishop!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    blackBishop!!.localPosition = getLocalPosition(index)
                }
                blackBishop!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                blackBishop!!.renderable = blackBishopRenderable
                nodeList.add(blackBishop)
            }
            "BLACK_QUEEN" -> {
                val blackQueen = Node()
                blackQueen!!.setParent(anchorNode)
                blackQueen!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    blackQueen!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    blackQueen!!.localPosition = getLocalPosition(index)
                }
                blackQueen!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                blackQueen!!.renderable = blackQueenRenderable
                nodeList.add(blackQueen)
            }
            "BLACK_KING" -> {
                val blackKing = Node()
                blackKing!!.setParent(anchorNode)
                blackKing!!.localScale = pieceScaleVector
                if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                    blackKing!!.localPosition = mirrorVector(getLocalPosition(index))
                } else {
                    blackKing!!.localPosition = getLocalPosition(index)
                }
                blackKing!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                    try {
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(mirrorVector(hitTestResult!!.node!!.localPosition)))

                        } else {
                            selectTileAtIndex(hitTestResult!!.node!!, vectorToIndex(hitTestResult!!.node!!.localPosition))
                        }
                    } catch (exception: Exception) {
                        Log.i(TAG, "on tap exception for piece node")
                    }
                }
                blackKing!!.renderable = blackKingRenderable
                nodeList.add(blackKing)
            }
        }
    }

    fun getPositionFromIndex(index:Int): String {
        var letterList = arrayListOf<Char>('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')
        val floorDiv = Math.floorDiv(index, 8)
        val number =  floorDiv + 1
        val letter = letterList.get(index - 8 * floorDiv)
        return "$letter$number"
    }

    private fun selectTileAtIndex (node:Node, tileIndex: Int) {
        val sideToMove = virtualBoard.sideToMove
//        if (isOnlineGame && localPlayerColor != sideToMove) {
//            return
//        }
        val pieceAtIndex = virtualBoard.getPiece(Square.squareAt(tileIndex))

        // Check if a Chess Piece was Previously Selected
        if (tileSelectedIndex == -1) {
            // Chess Piece not Previously Selected
            // Check if Selected Tile has a Chess Piece Belonging to Current Side to Move
            if (pieceAtIndex != Piece.NONE && pieceAtIndex.pieceSide == sideToMove) {
                // render selected tile
                val selected = Node()
                selected!!.setParent(anchorNode)
                selected!!.localScale = Vector3(0.005875f, 0.005875f, 0.005875f)
                selected!!.localPosition = node.localPosition
                selected!!.renderable = selectedRenderable
                nodeList.add(selected)

                // Save the Index of the Selected Tile
                tileSelectedIndex = tileIndex
                // Check for eligible moves
                val allLegalMovesCurrent = virtualBoard.legalMoves()
                // iterating through all the legal moves on the virtualBoard
                currentLegalMoves.clear()
                // add to list of piece's legal moves
                currentLegalMoves.addAll(allLegalMovesCurrent)

                for (eachMove in allLegalMovesCurrent) {
                    // if legal move is relevant to selected piece
                    if (Square.squareAt(tileSelectedIndex).toString().toLowerCase(Locale.ENGLISH) == eachMove.toString().substring(0,2)) {
                        // change colour of legal moves
//                        chessTiles[Square.values().indexOf(eachMove.to)].setBackgroundColor(Color.parseColor("#48D1CC"))
                        // render available moves
                        val tile = Node()
                        tile!!.setParent(anchorNode)
                        tile!!.localScale = Vector3(0.1175f, 0.1175f, 0.1175f)
                        if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                            tile!!.localPosition = mirrorVector(getLocalPosition(Square.values().indexOf(eachMove.to)))
                        } else {
                            tile!!.localPosition = getLocalPosition(Square.values().indexOf(eachMove.to))
                        }
                        tile!!.setOnTapListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? ->
                            if (isOnlineGameIntialized && localPlayerColor == Side.BLACK) {
                                selectTileAtIndex(hitTestResult.node!!, vectorToIndex(mirrorVector(hitTestResult.node!!.localPosition)))

                            } else {
                                selectTileAtIndex(hitTestResult.node!!, vectorToIndex(hitTestResult.node!!.localPosition))
                            }
                        }
                        tile!!.renderable = tileRenderable
                        nodeList.add(tile)

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

            // Check New Move Object
            var newMove = Move(Square.squareAt(tileSelectedIndex),Square.squareAt(tileIndex))
            if (sideToMove == Side.WHITE && newMove.to.rank == Rank.RANK_8 && virtualBoard.getPiece(Square.squareAt(tileSelectedIndex)) == Piece.WHITE_PAWN) {
                openPromotionDialog(sideToMove, newMove)
            } else if (sideToMove == Side.BLACK && newMove.to.rank == Rank.RANK_1 && virtualBoard.getPiece(Square.squareAt(tileSelectedIndex)) == Piece.BLACK_PAWN) {
                openPromotionDialog(sideToMove, newMove)
            }
            // Check if New Move is Legal
            if(newMove in currentLegalMoves) {
                virtualBoard.doMove(newMove)
                tts!!.speak("$squareSelectedIdx to $squareIdx", TextToSpeech.QUEUE_FLUSH, null,"")
                // Save virtualBoard state to virtualBoardHistoryLocal array if game is offline
//                if (!isOnlineGame) {
//                    virtualBoardHistoryLocal.add(virtualBoard.fen)
//                    Log.i("cliffen", virtualBoardHistoryLocal.toString())
//                }
                renderBoardState()
                tileSelectedIndex = -1

                afterMoveHandler()

                if (isOnlineGame){
                    sendvirtualBoardStateOnline()
                }
            }
        }
    }

    private fun sendvirtualBoardStateOnline() {
        val TAG = "cliffen"
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    boardHistory = document.data!!["boardHistory"] as ArrayList<String>
                    Log.i("cliffen before", boardHistory.toString())
                    boardHistory.add(virtualBoard.fen)
                    Log.i("cliffen after", boardHistory.toString())
                    val boardData = hashMapOf(
                        "boardState" to virtualBoard.fen,
                        "boardHistory" to boardHistory)
                    roomRef.set(boardData, SetOptions.merge())
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
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
            dialogShown = true
    }

    private fun movePieceAndPromote(move: Move, piece: Piece){
        val newMove = Move(move.from, move.to, piece)
        if(newMove in currentLegalMoves) {
            virtualBoard.doMove(newMove)

            // Save board state to boardHistoryLocal array if game is offline
            if (!isOnlineGame) {
                boardHistoryLocal.add(virtualBoard.fen)
                Log.i("cliffen", boardHistoryLocal.toString())
            }
            renderBoardState()
            tileSelectedIndex = -1

            afterMoveHandler()

            if (isOnlineGame){
                sendvirtualBoardStateOnline()
            }
        }
    }

    override fun onBackPressed() {
        if (dialogShown) {
            val it = Intent()
            it.putExtra("board", virtualBoard.fen)
            setResult(1001, it)
            super.onBackPressed()
        } else {
            val it = Intent()
            it.putExtra("board", virtualBoard.fen)
            setResult(1500, it)
            super.onBackPressed()
        }
    }

    fun returnToVirtualBoard(view:View) {
        onBackPressed()
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

}
