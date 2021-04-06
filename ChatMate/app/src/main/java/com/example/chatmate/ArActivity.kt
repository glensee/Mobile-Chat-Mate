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
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.util.*
import kotlin.collections.ArrayList


/**
 * This is augmented reality chess built using Google's HelloSceneform as a base.
 */
class ArActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var gameStarted = false
    private var boardRenderable: ModelRenderable? = null
    private var pawnRenderable: ModelRenderable? = null
//    private var pawn1Renderable: ModelRenderable? = null
//    private var pawn2Renderable: ModelRenderable? = null
//    private var pawn3Renderable: ModelRenderable? = null
//    private var pawn4Renderable: ModelRenderable? = null
//    private var pawn5Renderable: ModelRenderable? = null
//    private var pawn6Renderable: ModelRenderable? = null
//    private var pawn7Renderable: ModelRenderable? = null
//    private var pawn8Renderable: ModelRenderable? = null
    private var bishopRenderable: ModelRenderable? = null
//    private var bishop2Renderable: ModelRenderable? = null
    private var queenRenderable: ModelRenderable? = null
    private var kingRenderable: ModelRenderable? = null
//    private var rook1Renderable: ModelRenderable? = null
    private var rookRenderable: ModelRenderable? = null
    private var knightRenderable: ModelRenderable? = null
//    private var knight2Renderable: ModelRenderable? = null

    private var board: Node? = null
    private val halfBoard = 0.02937f
    private val pieceScaleVector = Vector3(0.025f, 0.025f, 0.025f)
    private val boardScaleVector = Vector3(1.7f, 1.7f, 1.7f)
    private val pieceNames = arrayListOf("pawn", "rook", "knight", "bishop","queen", "king") as ArrayList<String>


    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_ar)
        arFragment = getSupportFragmentManager().findFragmentById(R.id.ux_fragment) as ArFragment

        // Build pawn renderable
            ModelRenderable.builder()
                    .setSource(this, R.raw.whitepawn)
                    .build()
                    .thenAccept { renderable -> pawnRenderable = renderable }
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
                .thenAccept { renderable -> rookRenderable = renderable }
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
                .thenAccept { renderable -> knightRenderable = renderable }
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
                .thenAccept { renderable -> bishopRenderable = renderable }
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
                .thenAccept { renderable -> queenRenderable = renderable }
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
                .thenAccept { renderable -> kingRenderable = renderable }
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

        // Create the pawn and add it to the anchor.
        val pawn1 = Node()
        pawn1!!.setParent(anchorNode)
        pawn1!!.localScale = pieceScaleVector
        pawn1!!.localPosition = getPosition("pawn", 1, "white")
        pawn1!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn2 = Node()
        pawn2!!.setParent(anchorNode)
        pawn2!!.localScale = pieceScaleVector
        pawn2!!.localPosition = getPosition("pawn", 2, "white")
        pawn2!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn3 = Node()
        pawn3!!.setParent(anchorNode)
        pawn3!!.localScale = pieceScaleVector
        pawn3!!.localPosition = getPosition("pawn", 3, "white")
        pawn3!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn4 = Node()
        pawn4!!.setParent(anchorNode)
        pawn4!!.localScale = pieceScaleVector
        pawn4!!.localPosition = getPosition("pawn", 4, "white")
        pawn4!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn5 = Node()
        pawn5!!.setParent(anchorNode)
        pawn5!!.localScale = pieceScaleVector
        pawn5!!.localPosition = getPosition("pawn", 5, "white")
        pawn5!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn6 = Node()
        pawn6!!.setParent(anchorNode)
        pawn6!!.localScale = pieceScaleVector
        pawn6!!.localPosition = getPosition("pawn", 6, "white")
        pawn6!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn7 = Node()
        pawn7!!.setParent(anchorNode)
        pawn7!!.localScale = pieceScaleVector
        pawn7!!.localPosition = getPosition("pawn", 7, "white")
        pawn7!!.renderable = pawnRenderable

        // Create the pawn and add it to the anchor.
        val pawn8 = Node()
        pawn8!!.setParent(anchorNode)
        pawn8!!.localScale = pieceScaleVector
        pawn8!!.localPosition = getPosition("pawn", 8, "white")
        pawn8!!.renderable = pawnRenderable

        // Create the rook and add it to the anchor.
        val rook1 = Node()
        rook1!!.setParent(anchorNode)
        rook1!!.localScale = pieceScaleVector
        rook1!!.localPosition = getPosition("rook", 1, "white")
        rook1!!.renderable = rookRenderable

        // Create the rook and add it to the anchor.
        val rook2 = Node()
        rook2!!.setParent(anchorNode)
        rook2!!.localScale = pieceScaleVector
        rook2!!.localPosition = getPosition("rook", 2, "white")
        rook2!!.renderable = rookRenderable

        // Create the knight and add it to the anchor.
        val knight1 = Node()
        knight1!!.setParent(anchorNode)
        knight1!!.localScale = pieceScaleVector
        knight1!!.localPosition = getPosition("knight", 1, "white")
        knight1!!.renderable = knightRenderable

        // Create the knight and add it to the anchor.
        val knight2 = Node()
        knight2!!.setParent(anchorNode)
        knight2!!.localScale = pieceScaleVector
        knight2!!.localPosition = getPosition("knight", 2, "white")
        knight2!!.renderable = knightRenderable

        // Create the bishop and add it to the anchor.
        val bishop1 = Node()
        bishop1!!.setParent(anchorNode)
        bishop1!!.localScale = pieceScaleVector
        bishop1!!.localPosition = getPosition("bishop", 1, "white")
        bishop1!!.renderable = bishopRenderable

        // Create the bishop and add it to the anchor.
        val bishop2 = Node()
        bishop2!!.setParent(anchorNode)
        bishop2!!.localScale = pieceScaleVector
        bishop2!!.localPosition = getPosition("bishop", 2, "white")
        bishop2!!.renderable = bishopRenderable

        // Create the bishop and add it to the anchor.
        val queen = Node()
        queen!!.setParent(anchorNode)
        queen!!.localScale = pieceScaleVector
        queen!!.localPosition = getPosition("queen", 1, "white")
        queen!!.renderable = queenRenderable

        // Create the bishop and add it to the anchor.
        val king = Node()
        king!!.setParent(anchorNode)
        king!!.localScale = pieceScaleVector
        king!!.localPosition = getPosition("king", 1, "white")
        king!!.renderable = kingRenderable

    }

    fun getPosition(piece:String, position:Int, colour:String): Vector3 {
        val y = 0.020f
        var x = 0f
        var z = 0f
        when (colour) {
            "white" -> {
                when (piece) {
                    "pawn" -> {
                        z = 5 * halfBoard
                        x = (-7 + (position-1) * 2) * halfBoard
                    }

                    "rook" -> {
                        z = 7 * halfBoard
                        x = (-7 + (position-1) * 14 ) * halfBoard
                    }

                    "knight" -> {
                        z = 7 * halfBoard
                        x = (-5 + (position-1) * 10 ) * halfBoard

                    }

                    "bishop" -> {
                        z = 7 * halfBoard
                        x = (-3 + (position-1) * 6 ) * halfBoard

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
        }
        Log.i("cliffen", "$piece $position $x $y $z")
        return Vector3(x, y, z)
    }
}
