package com.example.chesslib;

import java.util.Random;

/**
 * Zobrist hashing: turns the current position into a single 64-bit number (a "fingerprint").
 *
 * How it works:
 *  - At startup, every (piece, square) pair gets its own random 64-bit number.
 *  - A position's fingerprint = XOR of the random numbers of every piece on the board,
 *    plus one extra number if it's white to move, plus one per castling right still available.
 *  - Two identical positions always give the same number.
 *    Two different positions give different numbers (a collision is astronomically unlikely).
 *
 * Plain Java only: works on desktop and Android.
 */
public final class Zobrist {

    // PIECE[pieceCode][square] -> random number. Uses your piece codes 1..12 (index 0 unused).
    private static final long[][] PIECE = new long[13][64];

    // XORed in when it's white's turn: same pieces but a different side to move = a different position.
    private static final long SIDE;

    // One number per castling right: white short, white long, black short, black long.
    private static final long[] CASTLE = new long[4];

    static {
        // Fixed seed: the numbers are the same every time the program starts (easier to debug).
        Random random = new Random(123456789L);
        for (int piece = 1; piece <= 12; piece++) {
            for (int square = 0; square < 64; square++) {
                PIECE[piece][square] = random.nextLong();
            }
        }
        SIDE = random.nextLong();
        for (int i = 0; i < 4; i++) {
            CASTLE[i] = random.nextLong();
        }
    }

    private Zobrist() { }

    /** Returns the fingerprint of the position currently stored in Board / MovementManager. */
    public static long hash() {
        long h = 0L;

        h ^= pieces(Board.whitePawns, 1);
        h ^= pieces(Board.whiteBishops, 2);
        h ^= pieces(Board.whiteRoocks, 3);
        h ^= pieces(Board.whiteQueen, 4);
        h ^= pieces(Board.whiteKnights, 5);
        h ^= pieces(Board.whiteKing, 6);

        h ^= pieces(Board.blackPawns, 7);
        h ^= pieces(Board.blackBishops, 8);
        h ^= pieces(Board.blackRoocks, 9);
        h ^= pieces(Board.blackQueen, 10);
        h ^= pieces(Board.blackKnights, 11);
        h ^= pieces(Board.blackKing, 12);

        MovementManager mm = MovementManager.INSTANCE;

        if (mm.turn) {
            h ^= SIDE;
        }

        // Castling rights change which moves are possible, so they are part of the position.
        if (!mm.WhiteKingHasBeenMoved && !mm.WhiteRightRookHasBeenMoved) h ^= CASTLE[0];
        if (!mm.WhiteKingHasBeenMoved && !mm.WhiteLeftRookHasBeenMoved)  h ^= CASTLE[1];
        if (!mm.blackKingHasBeenMoved && !mm.BlackRightRookHasBeenMoved) h ^= CASTLE[2];
        if (!mm.blackKingHasBeenMoved && !mm.BlackLeftRookHasBeenMoved)  h ^= CASTLE[3];

        return h;
    }

    /** XOR of the random numbers for every piece in one bitboard. */
    private static long pieces(long bitboard, int pieceCode) {
        long h = 0L;
        while (bitboard != 0L) {
            int square = Long.numberOfTrailingZeros(bitboard); // position of the lowest piece
            h ^= PIECE[pieceCode][square];
            bitboard &= bitboard - 1;                          // remove that piece, continue
        }
        return h;
    }
}
