package com.example.chatmate

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.util.*


/**
 * This is augmented reality chess built using Google's HelloSceneform as a base.
 */
class ArActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var gameStarted = false
    private var boardRenderable: ModelRenderable? = null
    
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

    private var board: Node? = null
    private val halfBoard = 0.02937f
    private val pieceScaleVector = Vector3(0.025f, 0.025f, 0.025f)
    private val boardScaleVector = Vector3(1.7f, 1.7f, 1.7f)
    private val pieceNames = arrayListOf("pawn", "rook", "knight", "bishop", "queen", "king") as ArrayList<String>


    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_ar)
        arFragment = getSupportFragmentManager().findFragmentById(R.id.ux_fragment) as ArFragment

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
            val anchorNode =
                AnchorNode(anchor)
            anchorNode.setParent(arFragment!!.arSceneView.scene)
            if (board != null) return@setOnTapArPlaneListener

            // for online multiplayer
            if (gameStarted) {
                // render on board according to current board state
            } else {
                renderFreshBoard(anchorNode)
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
        val whitePawn1 = Node()
        whitePawn1!!.setParent(anchorNode)
        whitePawn1!!.localScale = pieceScaleVector
        whitePawn1!!.localPosition = getPosition("pawn", 1, "white")
        whitePawn1!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn2 = Node()
        whitePawn2!!.setParent(anchorNode)
        whitePawn2!!.localScale = pieceScaleVector
        whitePawn2!!.localPosition = getPosition("pawn", 2, "white")
        whitePawn2!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn3 = Node()
        whitePawn3!!.setParent(anchorNode)
        whitePawn3!!.localScale = pieceScaleVector
        whitePawn3!!.localPosition = getPosition("pawn", 3, "white")
        whitePawn3!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn4 = Node()
        whitePawn4!!.setParent(anchorNode)
        whitePawn4!!.localScale = pieceScaleVector
        whitePawn4!!.localPosition = getPosition("pawn", 4, "white")
        whitePawn4!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn5 = Node()
        whitePawn5!!.setParent(anchorNode)
        whitePawn5!!.localScale = pieceScaleVector
        whitePawn5!!.localPosition = getPosition("pawn", 5, "white")
        whitePawn5!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn6 = Node()
        whitePawn6!!.setParent(anchorNode)
        whitePawn6!!.localScale = pieceScaleVector
        whitePawn6!!.localPosition = getPosition("pawn", 6, "white")
        whitePawn6!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn7 = Node()
        whitePawn7!!.setParent(anchorNode)
        whitePawn7!!.localScale = pieceScaleVector
        whitePawn7!!.localPosition = getPosition("pawn", 7, "white")
        whitePawn7!!.renderable = whitePawnRenderable

        // Create the pawn and add it to the anchor.
        val whitePawn8 = Node()
        whitePawn8!!.setParent(anchorNode)
        whitePawn8!!.localScale = pieceScaleVector
        whitePawn8!!.localPosition = getPosition("pawn", 8, "white")
        whitePawn8!!.renderable = whitePawnRenderable

        // Create the rook and add it to the anchor.
        val whiteRook1 = Node()
        whiteRook1!!.setParent(anchorNode)
        whiteRook1!!.localScale = pieceScaleVector
        whiteRook1!!.localPosition = getPosition("rook", 1, "white")
        whiteRook1!!.renderable = whiteRookRenderable

        // Create the rook and add it to the anchor.
        val whiteRook2 = Node()
        whiteRook2!!.setParent(anchorNode)
        whiteRook2!!.localScale = pieceScaleVector
        whiteRook2!!.localPosition = getPosition("rook", 2, "white")
        whiteRook2!!.renderable = whiteRookRenderable

        // Create the knight and add it to the anchor.
        val whiteKnight1 = Node()
        whiteKnight1!!.setParent(anchorNode)
        whiteKnight1!!.localScale = pieceScaleVector
        whiteKnight1.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteKnight1!!.localPosition = getPosition("knight", 1, "white")
        whiteKnight1!!.renderable = whiteKnightRenderable

        // Create the knight and add it to the anchor.
        val whiteKnight2 = Node()
        whiteKnight2!!.setParent(anchorNode)
        whiteKnight2!!.localScale = pieceScaleVector
        whiteKnight2.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteKnight2!!.localPosition = getPosition("knight", 2, "white")
        whiteKnight2!!.renderable = whiteKnightRenderable

        // Create the bishop and add it to the anchor.
        val whiteBishop1 = Node()
        whiteBishop1!!.setParent(anchorNode)
        whiteBishop1!!.localScale = pieceScaleVector
        whiteBishop1.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteBishop1!!.localPosition = getPosition("bishop", 1, "white")
        whiteBishop1!!.renderable = whiteBishopRenderable

        // Create the bishop and add it to the anchor.
        val whiteBishop2 = Node()
        whiteBishop2!!.setParent(anchorNode)
        whiteBishop2!!.localScale = pieceScaleVector
        whiteBishop2.setLocalRotation(Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f))
        whiteBishop2!!.localPosition = getPosition("bishop", 2, "white")
        whiteBishop2!!.renderable = whiteBishopRenderable

        // Create the bishop and add it to the anchor.
        val whiteQueen = Node()
        whiteQueen!!.setParent(anchorNode)
        whiteQueen!!.localScale = pieceScaleVector
        whiteQueen!!.localPosition = getPosition("queen", 1, "white")
        whiteQueen!!.renderable = whiteQueenRenderable

        // Create the bishop and add it to the anchor.
        val whiteKing = Node()
        whiteKing!!.setParent(anchorNode)
        whiteKing!!.localScale = pieceScaleVector
        whiteKing!!.localPosition = getPosition("king", 1, "white")
        whiteKing!!.renderable = whiteKingRenderable

        // BLACK
        // Create the pawn and add it to the anchor.
        val blackPawn1 = Node()
        blackPawn1!!.setParent(anchorNode)
        blackPawn1!!.localScale = pieceScaleVector
        blackPawn1!!.localPosition = getPosition("pawn", 1, "black")
        blackPawn1!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn2 = Node()
        blackPawn2!!.setParent(anchorNode)
        blackPawn2!!.localScale = pieceScaleVector
        blackPawn2!!.localPosition = getPosition("pawn", 2, "black")
        blackPawn2!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn3 = Node()
        blackPawn3!!.setParent(anchorNode)
        blackPawn3!!.localScale = pieceScaleVector
        blackPawn3!!.localPosition = getPosition("pawn", 3, "black")
        blackPawn3!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn4 = Node()
        blackPawn4!!.setParent(anchorNode)
        blackPawn4!!.localScale = pieceScaleVector
        blackPawn4!!.localPosition = getPosition("pawn", 4, "black")
        blackPawn4!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn5 = Node()
        blackPawn5!!.setParent(anchorNode)
        blackPawn5!!.localScale = pieceScaleVector
        blackPawn5!!.localPosition = getPosition("pawn", 5, "black")
        blackPawn5!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn6 = Node()
        blackPawn6!!.setParent(anchorNode)
        blackPawn6!!.localScale = pieceScaleVector
        blackPawn6!!.localPosition = getPosition("pawn", 6, "black")
        blackPawn6!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn7 = Node()
        blackPawn7!!.setParent(anchorNode)
        blackPawn7!!.localScale = pieceScaleVector
        blackPawn7!!.localPosition = getPosition("pawn", 7, "black")
        blackPawn7!!.renderable = blackPawnRenderable

        // Create the pawn and add it to the anchor.
        val blackPawn8 = Node()
        blackPawn8!!.setParent(anchorNode)
        blackPawn8!!.localScale = pieceScaleVector
        blackPawn8!!.localPosition = getPosition("pawn", 8, "black")
        blackPawn8!!.renderable = blackPawnRenderable

        // Create the rook and add it to the anchor.
        val blackRook1 = Node()
        blackRook1!!.setParent(anchorNode)
        blackRook1!!.localScale = pieceScaleVector
        blackRook1!!.localPosition = getPosition("rook", 1, "black")
        blackRook1!!.renderable = blackRookRenderable

        // Create the rook and add it to the anchor.
        val blackRook2 = Node()
        blackRook2!!.setParent(anchorNode)
        blackRook2!!.localScale = pieceScaleVector
        blackRook2!!.localPosition = getPosition("rook", 2, "black")
        blackRook2!!.renderable = blackRookRenderable

        // Create the knight and add it to the anchor.
        val blackKnight1 = Node()
        blackKnight1!!.setParent(anchorNode)
        blackKnight1!!.localScale = pieceScaleVector
        blackKnight1!!.localPosition = getPosition("knight", 1, "black")
        blackKnight1!!.renderable = blackKnightRenderable

        // Create the knight and add it to the anchor.
        val blackKnight2 = Node()
        blackKnight2!!.setParent(anchorNode)
        blackKnight2!!.localScale = pieceScaleVector
        blackKnight2!!.localPosition = getPosition("knight", 2, "black")
        blackKnight2!!.renderable = blackKnightRenderable

        // Create the bishop and add it to the anchor.
        val blackBishop1 = Node()
        blackBishop1!!.setParent(anchorNode)
        blackBishop1!!.localScale = pieceScaleVector
        blackBishop1!!.localPosition = getPosition("bishop", 1, "black")
        blackBishop1!!.renderable = blackBishopRenderable

        // Create the bishop and add it to the anchor.
        val blackBishop2 = Node()
        blackBishop2!!.setParent(anchorNode)
        blackBishop2!!.localScale = pieceScaleVector
        blackBishop2!!.localPosition = getPosition("bishop", 2, "black")
        blackBishop2!!.renderable = blackBishopRenderable

        // Create the bishop and add it to the anchor.
        val blackQueen = Node()
        blackQueen!!.setParent(anchorNode)
        blackQueen!!.localScale = pieceScaleVector
        blackQueen!!.localPosition = getPosition("queen", 1, "black")
        blackQueen!!.renderable = blackQueenRenderable

        // Create the bishop and add it to the anchor.
        val blackKing = Node()
        blackKing!!.setParent(anchorNode)
        blackKing!!.localScale = pieceScaleVector
        blackKing!!.localPosition = getPosition("king", 1, "black")
        blackKing!!.renderable = blackKingRenderable

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
}
