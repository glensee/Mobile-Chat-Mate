package com.example.chatmate

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chatmate.databinding.ActivityArBinding
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.google.android.material.imageview.ShapeableImageView
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.speechly.client.slu.Segment
import com.speechly.client.speech.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.math.floor


/**
 * This is augmented reality chess built using Google's HelloSceneform as a base.
 */
class ArActivity : AppCompatActivity(), TextToSpeech.OnInitListener  {
    // debugging variable
    private val TAG = "cliffen"

    private lateinit var binding: ActivityArBinding
    private var arFragment: ArFragment? = null
    private var gameStarted = false

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

    // node list
    private var nodeList = ArrayList<Node>()
    private lateinit var anchorNode: AnchorNode

    // white pieces nodes
    private var whitePawn1 = Node()
    private var whitePawn2 = Node()
    private var whitePawn3 = Node()
    private var whitePawn4 = Node()
    private var whitePawn5 = Node()
    private var whitePawn6 = Node()
    private var whitePawn7 = Node()
    private var whitePawn8 = Node()
    private var whiteRook1 = Node()
    private var whiteRook2 = Node()
    private var whiteKnight1 = Node()
    private var whiteKnight2 = Node()
    private var whiteBishop1 = Node()
    private var whiteBishop2 = Node()
    private var whiteQueen = Node()
    private var whiteKing = Node()
    
    // black pieces nodes
    private var blackPawn1 = Node()
    private var blackPawn2 = Node()
    private var blackPawn3 = Node()
    private var blackPawn4 = Node()
    private var blackPawn5 = Node()
    private var blackPawn6 = Node()
    private var blackPawn7 = Node()
    private var blackPawn8 = Node()
    private var blackRook1 = Node()
    private var blackRook2 = Node()
    private var blackKnight1 = Node()
    private var blackKnight2 = Node()
    private var blackBishop1 = Node()
    private var blackBishop2 = Node()
    private var blackQueen = Node()
    private var blackKing = Node()

    // board node
    private var board: Node? = null

    // virtual game variables
    private lateinit var boardHistory: ArrayList<String>
    private var boardHistoryLocal = ArrayList<String>()
    private lateinit var virtualBoard: Board
    private val currentLegalMoves = ArrayList<Move>()

    // board array to store node references
    private var referencesArray = ArrayList<String>()

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

        // Generate virtual board and history
        virtualBoard = Board()
        
        // sync reference array with virtual board
//        syncReferenceAndBoard()

        Log.i(TAG, "references array: $referencesArray")
        // Initialize local boardHistoryArray for local multiplayer
        boardHistoryLocal.add(virtualBoard.fen)

//        //Check for Online Game
//        roomId = intent.getStringExtra("roomId").toString()
//        if(roomId != "null" && !isOnlineGameIntialized) {
//            gameBinding.onlineGameHeader.visibility = View.VISIBLE
//            setupOnlineGame()
//        }

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





        arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (boardRenderable == null) return@setOnTapArPlaneListener

            // Create the Anchor.
            val anchor = hitResult.createAnchor()
            anchorNode =
                AnchorNode(anchor)
            anchorNode.setParent(arFragment!!.arSceneView.scene)
            if (board != null) return@setOnTapArPlaneListener

            // for online multiplayer
            if (gameStarted) {
                // render on board according to current board state
            } else {
                board = Node()
                board!!.setParent(anchorNode)
                board!!.localScale = boardScaleVector
                board!!.renderable = boardRenderable
//                renderFreshBoard(anchorNode)
                renderBoardState()
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

    fun renderFreshBoard(anchorNode: AnchorNode) {
        // Create the board and add it to the anchor.
        board = Node()
        board!!.setParent(anchorNode)
        board!!.localScale = boardScaleVector
        board!!.renderable = boardRenderable

        // WHITE
        // Create the pawn and add it to the anchor.
        whitePawn1!!.setParent(anchorNode)
        whitePawn1!!.localScale = pieceScaleVector
        whitePawn1!!.localPosition = getPosition("pawn", 1, "white")
        whitePawn1!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn2!!.setParent(anchorNode)
        whitePawn2!!.localScale = pieceScaleVector
        whitePawn2!!.localPosition = getPosition("pawn", 2, "white")
        whitePawn2!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn3!!.setParent(anchorNode)
        whitePawn3!!.localScale = pieceScaleVector
        whitePawn3!!.localPosition = getPosition("pawn", 3, "white")
        whitePawn3!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn4!!.setParent(anchorNode)
        whitePawn4!!.localScale = pieceScaleVector
        whitePawn4!!.localPosition = getPosition("pawn", 4, "white")
        whitePawn4!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn5!!.setParent(anchorNode)
        whitePawn5!!.localScale = pieceScaleVector
        whitePawn5!!.localPosition = getPosition("pawn", 5, "white")
        whitePawn5!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn6!!.setParent(anchorNode)
        whitePawn6!!.localScale = pieceScaleVector
        whitePawn6!!.localPosition = getPosition("pawn", 6, "white")
        whitePawn6!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn7!!.setParent(anchorNode)
        whitePawn7!!.localScale = pieceScaleVector
        whitePawn7!!.localPosition = getPosition("pawn", 7, "white")
        whitePawn7!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        whitePawn8!!.setParent(anchorNode)
        whitePawn8!!.localScale = pieceScaleVector
        whitePawn8!!.localPosition = getPosition("pawn", 8, "white")
        whitePawn8!!.renderable = whitePawnRenderable

        // Create the rook and add it to the anchor.
        whiteRook1!!.setParent(anchorNode)
        whiteRook1!!.localScale = pieceScaleVector
        whiteRook1!!.localPosition = getPosition("rook", 1, "white")
        whiteRook1!!.renderable = whiteRookRenderable

        // Create the rook and add it to the anchor.
        whiteRook2!!.setParent(anchorNode)
        whiteRook2!!.localScale = pieceScaleVector
        whiteRook2!!.localPosition = getPosition("rook", 2, "white")
        whiteRook2!!.renderable = whiteRookRenderable

        // Create the knight and add it to the anchor.
        whiteKnight1!!.setParent(anchorNode)
        whiteKnight1!!.localScale = pieceScaleVector
        whiteKnight1.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteKnight1!!.localPosition = getPosition("knight", 1, "white")
        whiteKnight1!!.renderable = whiteKnightRenderable

        // Create the knight and add it to the anchor.
        whiteKnight2!!.setParent(anchorNode)
        whiteKnight2!!.localScale = pieceScaleVector
        whiteKnight2.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteKnight2!!.localPosition = getPosition("knight", 2, "white")
        whiteKnight2!!.renderable = whiteKnightRenderable

        // Create the bishop and add it to the anchor.
        whiteBishop1!!.setParent(anchorNode)
        whiteBishop1!!.localScale = pieceScaleVector
        whiteBishop1.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteBishop1!!.localPosition = getPosition("bishop", 1, "white")
        whiteBishop1!!.renderable = whiteBishopRenderable

        // Create the bishop and add it to the anchor.
        whiteBishop2!!.setParent(anchorNode)
        whiteBishop2!!.localScale = pieceScaleVector
        whiteBishop2.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteBishop2!!.localPosition = getPosition("bishop", 2, "white")
        whiteBishop2!!.renderable = whiteBishopRenderable

        // Create the bishop and add it to the anchor.
        whiteQueen!!.setParent(anchorNode)
        whiteQueen!!.localScale = pieceScaleVector
        whiteQueen!!.localPosition = getPosition("queen", 1, "white")
        whiteQueen!!.renderable = whiteQueenRenderable

        // Create the bishop and add it to the anchor.
        whiteKing!!.setParent(anchorNode)
        whiteKing!!.localScale = pieceScaleVector
        whiteKing!!.localPosition = getPosition("king", 1, "white")
        whiteKing!!.renderable = whiteKingRenderable

        // BLACK
        // Create the pawn and add it to the anchor.
        blackPawn1!!.setParent(anchorNode)
        blackPawn1!!.localScale = pieceScaleVector
        blackPawn1!!.localPosition = getPosition("pawn", 1, "black")
        blackPawn1!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn2!!.setParent(anchorNode)
        blackPawn2!!.localScale = pieceScaleVector
        blackPawn2!!.localPosition = getPosition("pawn", 2, "black")
        blackPawn2!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn3!!.setParent(anchorNode)
        blackPawn3!!.localScale = pieceScaleVector
        blackPawn3!!.localPosition = getPosition("pawn", 3, "black")
        blackPawn3!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn4!!.setParent(anchorNode)
        blackPawn4!!.localScale = pieceScaleVector
        blackPawn4!!.localPosition = getPosition("pawn", 4, "black")
        blackPawn4!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn5!!.setParent(anchorNode)
        blackPawn5!!.localScale = pieceScaleVector
        blackPawn5!!.localPosition = getPosition("pawn", 5, "black")
        blackPawn5!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn6!!.setParent(anchorNode)
        blackPawn6!!.localScale = pieceScaleVector
        blackPawn6!!.localPosition = getPosition("pawn", 6, "black")
        blackPawn6!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn7!!.setParent(anchorNode)
        blackPawn7!!.localScale = pieceScaleVector
        blackPawn7!!.localPosition = getPosition("pawn", 7, "black")
        blackPawn7!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        blackPawn8!!.setParent(anchorNode)
        blackPawn8!!.localScale = pieceScaleVector
        blackPawn8!!.localPosition = getPosition("pawn", 8, "black")
        blackPawn8!!.renderable = blackPawnRenderable

        // Create the rook and add it to the anchor.
        blackRook1!!.setParent(anchorNode)
        blackRook1!!.localScale = pieceScaleVector
        blackRook1!!.localPosition = getPosition("rook", 1, "black")
        blackRook1!!.renderable = blackRookRenderable

        // Create the rook and add it to the anchor.
        blackRook2!!.setParent(anchorNode)
        blackRook2!!.localScale = pieceScaleVector
        blackRook2!!.localPosition = getPosition("rook", 2, "black")
        blackRook2!!.renderable = blackRookRenderable

        // Create the knight and add it to the anchor.
        blackKnight1!!.setParent(anchorNode)
        blackKnight1!!.localScale = pieceScaleVector
        blackKnight1!!.localPosition = getPosition("knight", 1, "black")
        blackKnight1!!.renderable = blackKnightRenderable

        // Create the knight and add it to the anchor.
        blackKnight2!!.setParent(anchorNode)
        blackKnight2!!.localScale = pieceScaleVector
        blackKnight2!!.localPosition = getPosition("knight", 2, "black")
        blackKnight2!!.renderable = blackKnightRenderable

        // Create the bishop and add it to the anchor.
        blackBishop1!!.setParent(anchorNode)
        blackBishop1!!.localScale = pieceScaleVector
        blackBishop1!!.localPosition = getPosition("bishop", 1, "black")
        blackBishop1!!.renderable = blackBishopRenderable

        // Create the bishop and add it to the anchor.
        blackBishop2!!.setParent(anchorNode)
        blackBishop2!!.localScale = pieceScaleVector
        blackBishop2!!.localPosition = getPosition("bishop", 2, "black")
        blackBishop2!!.renderable = blackBishopRenderable

        // Create the bishop and add it to the anchor.
        blackQueen!!.setParent(anchorNode)
        blackQueen!!.localScale = pieceScaleVector
        blackQueen!!.localPosition = getPosition("queen", 1, "black")
        blackQueen!!.renderable = blackQueenRenderable

        // Create the bishop and add it to the anchor.
        blackKing!!.setParent(anchorNode)
        blackKing!!.localScale = pieceScaleVector
        blackKing!!.localPosition = getPosition("king", 1, "black")
        blackKing!!.renderable = blackKingRenderable

        nodeList.add(whitePawn1)
        nodeList.add(whitePawn2)
        nodeList.add(whitePawn3)
        nodeList.add(whitePawn4)
        nodeList.add(whitePawn5)
        nodeList.add(whitePawn6)
        nodeList.add(whitePawn7)
        nodeList.add(whitePawn8)
        nodeList.add(whiteRook1)
        nodeList.add(whiteRook2)
        nodeList.add(whiteKnight1)
        nodeList.add(whiteKnight2)
        nodeList.add(whiteBishop1)
        nodeList.add(whiteBishop2)
        nodeList.add(whiteQueen)
        nodeList.add(whiteKing)
        nodeList.add(blackPawn1)
        nodeList.add(blackPawn2)
        nodeList.add(blackPawn3)
        nodeList.add(blackPawn4)
        nodeList.add(blackPawn5)
        nodeList.add(blackPawn6)
        nodeList.add(blackPawn7)
        nodeList.add(blackPawn8)
        nodeList.add(blackRook1)
        nodeList.add(blackRook2)
        nodeList.add(blackKnight1)
        nodeList.add(blackKnight2)
        nodeList.add(blackBishop1)
        nodeList.add(blackBishop2)
        nodeList.add(blackQueen)
        nodeList.add(blackKing)
    }

    fun getPosition(piece: String, position: Int, colour: String): Vector3 {
        val y = 0.020f
        var x = 0f
        var z = 0f
        when (colour) {
            "white" -> {
                when (piece) {
                    "pawn" -> {
                        z = 5 * halfBoard
                        x = (-7 + (position - 1) * 2) * halfBoard
                    }

                    "rook" -> {
                        z = 7 * halfBoard
                        x = (-7 + (position - 1) * 14) * halfBoard
                    }

                    "knight" -> {
                        z = 7 * halfBoard
                        x = (-5 + (position - 1) * 10) * halfBoard

                    }

                    "bishop" -> {
                        z = 7 * halfBoard
                        x = (-3 + (position - 1) * 6) * halfBoard

                    }

                    "queen" -> {
                        z = 7 * halfBoard
                        x = -halfBoard
                    }

                    "king" -> {
                        z = halfBoard * 7
                        x = halfBoard
                    }
                }
            }

            "black" -> {
                when (piece) {
                    "pawn" -> {
                        z = -5 * halfBoard
                        x = (-7 + (position - 1) * 2) * halfBoard
                    }

                    "rook" -> {
                        z = -7 * halfBoard
                        x = (-7 + (position - 1) * 14) * halfBoard
                    }

                    "knight" -> {
                        z = -7 * halfBoard
                        x = (-5 + (position - 1) * 10) * halfBoard

                    }

                    "bishop" -> {
                        z = -7 * halfBoard
                        x = (-3 + (position - 1) * 6) * halfBoard

                    }

                    "queen" -> {
                        z = -7 * halfBoard
                        x = -halfBoard
                    }

                    "king" -> {
                        z = -7 * halfBoard
                        x = halfBoard
                    }
                }
            }
        }
        Log.i("cliffen", "$colour $piece $position $x $y $z")
        return Vector3(x, y, z)
    }

//    fun movePiece(from:String, end: String) {
//        Log.i(TAG, "moving piece...")
//        val fromNode = getNodeAtPosition(from)
//        val toNode = getNodeAtPosition(end)
//        val durationInMilliseconds = 1000L
//        val begin = fromNode.localPosition
//        val end = convertToVector(end)
//        val test = toNode == Node()
//        Log.i(TAG, "Node equivalence test: $test")
//        val objectAnimator = ObjectAnimator.ofObject(fromNode, "localPosition", Vector3Evaluator(), begin, end)
//        objectAnimator.duration = durationInMilliseconds
//        objectAnimator.start()
//
//    }

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
//                if (isOnlineGame){
//                    sendvirtualBoardStateOnline()
//                }
                virtualBoard.doMove(newMove)
//                movePiece(from.toString(), to.toString())
                
                tts!!.speak("$from to $to", TextToSpeech.QUEUE_FLUSH, null,"")
                renderBoardState()
                currentLegalMoves.clear()
                currentLegalMoves.addAll(virtualBoard.legalMoves())
                binding.voiceResultTextField.text = "Tap and hold to speak"
//                afterMoveHandler()
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

//    private fun positionToIndex(position: String): Int {
//        var letterList = arrayListOf<Char>('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')
//        val debug1 = position[1].toString().toInt()
//        Log.i(TAG, "debug1: $debug1")
//        val result = 8 * position[1].toString().toInt() - (8 - letterList.indexOf(position[0]) )
//        Log.i(TAG, "position: $position")
//        Log.i(TAG, "result index: $result")
//        return result
//    }

//    private fun afterMoveHandler() {
//        val myDialog = Dialog(this)
//        myDialog.setContentView(R.layout.game_finish_popup)
//        myDialog.setCanceledOnTouchOutside(false)
//        myDialog.setCancelable(false)
//        myDialog.findViewById<Button>(R.id.returnBtn).setOnClickListener{
//            //remove firestore snapshot and success listener
//            if (isOnlineGame) {
//                snapshotListener.remove()
//                deleteRoomDocument()
//                roomLeft = true
//            }
//            finish()
//        }
//        if (virtualBoard.isMated) {
//            myDialog.show()
//            val winner = virtualBoard.sideToMove.flip().toString().toLowerCase(Locale.ENGLISH).capitalize(Locale.ENGLISH)
//            val result = "$winner Wins"
//            if (!isOnlineGame) {
//                savevirtualBoardHistory(winner)
//            }
//            myDialog.findViewById<TextView>(R.id.resultText).setText(result)
//            if(virtualBoard.sideToMove == Side.BLACK){
//                myDialog.findViewById<ShapeableImageView>(R.id.whiteAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
//            } else {
//                myDialog.findViewById<ShapeableImageView>(R.id.blackAvatar).setBackgroundColor(ContextCompat.getColor(this, R.color.text_color_green))
//            }
//        } else if (virtualBoard.isDraw) {
//            if (virtualBoard.isRepetition) {
//                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
//                if (!isOnlineGame) {
//                    savevirtualBoardHistory("")
//                }
//                myDialog.show()
//            } else if (virtualBoard.isInsufficientMaterial) {
//                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
//                if (!isOnlineGame) {
//                    savevirtualBoardHistory("")
//                }
//                myDialog.show()
//            } else if (virtualBoard.halfMoveCounter >= 100) {
//                myDialog.findViewById<TextView>(R.id.resultText).setText("Match Draw")
//                if (!isOnlineGame) {
//                    savevirtualBoardHistory("")
//                }
//                myDialog.show()
//            }
//            else if (virtualBoard.isStaleMate){
//                myDialog.findViewById<TextView>(R.id.resultText).setText("Stale Mate")
//                if (!isOnlineGame) {
//                    savevirtualBoardHistory("")
//                }
//                myDialog.show()
//            }
//        }
//    }


//    private fun getNodeAtPosition(position: String): Node {
//        val nodeName = referencesArray[positionToIndex(position)]
//        when (nodeName) {
//            "WHITE_ROOK1" -> {
//                return whiteRook1
//            }
//
//            "WHITE_ROOK2" -> {
//                return whiteRook2
//            }
//
//            "WHITE_KNIGHT1" -> {
//                return whiteKnight1
//            }
//
//            "WHITE_KNIGHT2" -> {
//                return whiteKnight2
//            }
//
//            "WHITE_BISHOP1" -> {
//                return whiteBishop1
//            }
//
//            "WHITE_BISHOP2" -> {
//                return whiteBishop2
//            }
//
//            "WHITE_QUEEN" -> {
//                return whiteQueen
//            }
//
//            "WHITE_KING" -> {
//                return whiteKing
//            }
//            
//            "WHITE_PAWN1" -> {
//                return whitePawn1
//            }
//
//            "WHITE_PAWN2" -> {
//                return whitePawn2
//            }
//
//            "WHITE_PAWN3" -> {
//                return whitePawn3
//            }
//
//            "WHITE_PAWN4" -> {
//                return whitePawn4
//            }
//
//            "WHITE_PAWN5" -> {
//                return whitePawn5
//            }
//
//            "WHITE_PAWN6" -> {
//                return whitePawn6
//            }
//
//            "WHITE_PAWN7" -> {
//                return whitePawn7
//            }
//
//            "WHITE_PAWN8" -> {
//                return whitePawn8
//            }
//
//            "BLACK_ROOK1" -> {
//                return blackRook1
//            }
//
//            "BLACK_ROOK2" -> {
//                return blackRook2
//            }
//
//            "BLACK_KNIGHT1" -> {
//                return blackKnight1
//            }
//
//            "BLACK_KNIGHT2" -> {
//                return blackKnight2
//            }
//
//            "BLACK_BISHOP1" -> {
//                return blackBishop1
//            }
//
//            "BLACK_BISHOP2" -> {
//                return blackBishop2
//            }
//
//            "BLACK_QUEEN" -> {
//                return blackQueen
//            }
//
//            "BLACK_KING" -> {
//                return blackKing
//            }
//
//            "BLACK_PAWN1" -> {
//                return blackPawn1
//            }
//
//            "BLACK_PAWN2" -> {
//                return blackPawn2
//            }
//
//            "BLACK_PAWN3" -> {
//                return blackPawn3
//            }
//
//            "BLACK_PAWN4" -> {
//                return blackPawn4
//            }
//
//            "BLACK_PAWN5" -> {
//                return blackPawn5
//            }
//
//            "BLACK_PAWN6" -> {
//                return blackPawn6
//            }
//
//            "BLACK_PAWN7" -> {
//                return blackPawn7
//            }
//
//            "BLACK_PAWN8" -> {
//                return blackPawn8
//            }
//
//            "None" -> {
//                return Node()
//            }
//        }
//        return Node()
//    }
//    
//    private fun syncReferenceAndBoard() {
//        val arrayBoard = virtualBoard.boardToArray()
//        val tempReferencesArray = ArrayList<String>()
//        // Create references array for AR translation
//        var pawnCounter = 1
//        var rookCounter = 1
//        var knightCounter = 1
//        var bishopCounter = 1
//
//        for (i in 0 until 64) {
//            if (arrayBoard[i].value().contains("QUEEN") || arrayBoard[i].value().contains("KING")) {
//                tempReferencesArray.add(arrayBoard[i].value())
//            } else if (arrayBoard[i].value().contains("PAWN")) {
//                tempReferencesArray.add(arrayBoard[i].value()+pawnCounter)
//                if (pawnCounter == 8) {
//                    pawnCounter = 1
//                } else {
//                    pawnCounter ++
//                }
//            } else if (arrayBoard[i].value().contains("ROOK")) {
//                tempReferencesArray.add(arrayBoard[i].value()+rookCounter)
//                if (rookCounter == 2) {
//                    rookCounter = 1
//                } else {
//                    rookCounter ++
//                }
//            } else if (arrayBoard[i].value().contains("KNIGHT")) {
//                tempReferencesArray.add(arrayBoard[i].value()+knightCounter)
//                if (knightCounter == 2) {
//                    knightCounter = 1
//                } else {
//                    knightCounter ++
//                }
//            } else if (arrayBoard[i].value().contains("BISHOP")) {
//                tempReferencesArray.add(arrayBoard[i].value()+bishopCounter)
//                if (bishopCounter == 2) {
//                    bishopCounter = 1
//                } else {
//                    bishopCounter ++
//                }
//            } else {
//                tempReferencesArray.add(arrayBoard[i].value())
//            }
//        }
//
//        referencesArray = tempReferencesArray
//    }

    private fun renderBoardState () {
        Log.i("cliffen", "render 1")
        // Get the Current State of the Chess Board in an Array Sequence
        // Each Index Represents a Tile on the virtualBoard and The Item Represents the Piece Type
        val currentvirtualBoardStateArray = virtualBoard.boardToArray()

        // remove all nodes on board
        for (node: Node in nodeList) {
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
        Log.i(TAG, "Position: $position X: $x Y: $y Z: $z")
        return Vector3(x,y,z)
    }

    private fun getLocalPosition (index: Int): Vector3 {
        val position = getPositionFromIndex(index)
        return convertToVector(position)
    }

    private fun renderNode(piece:String, index: Int) {
        when (piece) {
            "None" -> return
            "WHITE_ROOK" -> {
                val whiteRook = Node()
                whiteRook!!.setParent(anchorNode)
                whiteRook!!.localScale = pieceScaleVector
                whiteRook!!.localPosition = getLocalPosition(index)
                whiteRook!!.renderable = whiteRookRenderable
                nodeList.add(whiteRook)
            }
            "WHITE_KNIGHT" -> {
                val whiteKnight = Node()
                whiteKnight!!.setParent(anchorNode)
                whiteKnight!!.localScale = pieceScaleVector
                whiteKnight.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                whiteKnight!!.localPosition = getLocalPosition(index)
                whiteKnight!!.renderable = whiteKnightRenderable
                nodeList.add(whiteKnight)
            }
            "WHITE_BISHOP" -> {
                val whiteBishop = Node()
                whiteBishop!!.setParent(anchorNode)
                whiteBishop!!.localScale = pieceScaleVector
                whiteBishop.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
                whiteBishop!!.localPosition = getLocalPosition(index)
                whiteBishop!!.renderable = whiteBishopRenderable
                nodeList.add(whiteBishop)
            }
            "WHITE_QUEEN" -> {
                val whiteQueen = Node()
                whiteQueen!!.setParent(anchorNode)
                whiteQueen!!.localScale = pieceScaleVector
                whiteQueen!!.localPosition = getLocalPosition(index)
                whiteQueen!!.renderable = whiteQueenRenderable
                nodeList.add(whiteQueen)
            }
            "WHITE_KING" -> {
                val whiteKing = Node()
                whiteKing!!.setParent(anchorNode)
                whiteKing!!.localScale = pieceScaleVector
                whiteKing!!.localPosition = getLocalPosition(index)
                whiteKing!!.renderable = whiteKingRenderable
                nodeList.add(whiteKing)
            }
            "WHITE_PAWN" -> {
                val whitePawn = Node()
                whitePawn!!.setParent(anchorNode)
                whitePawn!!.localScale = pieceScaleVector
                whitePawn!!.localPosition = getLocalPosition(index)
                whitePawn!!.renderable = whitePawnRenderable
                nodeList.add(whitePawn)
            }
            "BLACK_PAWN" -> {
                val blackPawn = Node()
                blackPawn!!.setParent(anchorNode)
                blackPawn!!.localScale = pieceScaleVector
                blackPawn!!.localPosition = getLocalPosition(index)
                blackPawn!!.renderable = blackPawnRenderable
                nodeList.add(blackPawn)
            }
            "BLACK_ROOK" -> {
                val blackRook = Node()
                blackRook!!.setParent(anchorNode)
                blackRook!!.localScale = pieceScaleVector
                blackRook!!.localPosition = getLocalPosition(index)
                blackRook!!.renderable = blackRookRenderable
                nodeList.add(blackRook)
            }
            "BLACK_KNIGHT" -> {
                val blackKnight = Node()
                blackKnight!!.setParent(anchorNode)
                blackKnight!!.localScale = pieceScaleVector
                blackKnight!!.localPosition = getLocalPosition(index)
                blackKnight!!.renderable = blackKnightRenderable
                nodeList.add(blackKnight)
            }
            "BLACK_BISHOP" -> {
                val blackBishop = Node()
                blackBishop!!.setParent(anchorNode)
                blackBishop!!.localScale = pieceScaleVector
                blackBishop!!.localPosition = getLocalPosition(index)
                blackBishop!!.renderable = blackBishopRenderable
                nodeList.add(blackBishop)
            }
            "BLACK_QUEEN" -> {
                val blackQueen = Node()
                blackQueen!!.setParent(anchorNode)
                blackQueen!!.localScale = pieceScaleVector
                blackQueen!!.localPosition = getLocalPosition(index)
                blackQueen!!.renderable = blackQueenRenderable
                nodeList.add(blackQueen)
            }
            "BLACK_KING" -> {
                val blackKing = Node()
                blackKing!!.setParent(anchorNode)
                blackKing!!.localScale = pieceScaleVector
                blackKing!!.localPosition = getLocalPosition(index)
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
}
