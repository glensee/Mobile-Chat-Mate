package com.example.chatmate

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList

class MoveListLocal() : MoveList() {
    override fun decodeSan(board: Board?, san: String?, side: Side?): Move {
        return super.decodeSan(board, san, side)
    }
    fun encodeSan(board: Board, newMove: Move): String {
        return encodeToSan(board, newMove)
    }
}