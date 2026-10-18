package com.example.chesslib;

/**
 * Transposition table: the engine's "notebook" of positions it has already calculated.
 *
 * For each position it stores:
 *  - key   : the Zobrist fingerprint (to make sure it's really the same position)
 *  - score : the result of the search
 *  - depth : how deep that search went (a deeper result is more reliable)
 *  - flag  : what kind of score it is (see below)
 *
 * Why a flag? Alpha-beta often stops searching a position early because it already knows
 * it's good enough / bad enough. Then the exact score is unknown, only a limit:
 *  - EXACT : the real score
 *  - LOWER : the real score is AT LEAST this value
 *  - UPPER : the real score is AT MOST this value
 *
 * Plain arrays instead of objects: faster and uses less memory (good for Android too).
 */
public final class TranspositionTable {

    public static final byte EXACT = 0;
    public static final byte LOWER = 1;
    public static final byte UPPER = 2;

    // Must be a power of 2. 1 << 20 = ~1 million entries = ~14 MB.
    // For older phones you can use 1 << 18 (~3.5 MB).
    private static final int SIZE = 1 << 20;

    private static final long[] keys   = new long[SIZE];
    private static final int[]  scores = new int[SIZE];
    private static final byte[] depths = new byte[SIZE];
    private static final byte[] flags  = new byte[SIZE];
    private static final Move[] moves = new Move[SIZE];

    // Mate scores depend on how far the mate is, so we don't store them (keeps things simple).


    private TranspositionTable() { }

    /** Which slot of the arrays a fingerprint goes into (the last 20 bits of the key). */
    private static int index(long key) {
        return (int) (key & (SIZE - 1));
    }

    /** True if this exact position is stored AND was searched at least this deep. */
    public static boolean has(long key, int depth) {
        int i = index(key);
        return keys[i] == key && depths[i] >= depth;
    }

    public static int score(long key) {
        return scores[index(key)];
    }

    public static Move getMove(long key) {
        int i = index(key);
        return keys[i] == key ? moves[i] : null;
    }

    public static byte flag(long key) {
        return flags[index(key)];
    }

    /** Writes a result. If the slot is taken by the same position, keeps the deeper search. */
    public static void store(long key, int depth, int score, byte flag, Move move) {
    int i = index(key);
    if (keys[i] != key || depth >= depths[i]) {
        keys[i] = key;
        scores[i] = score;
        depths[i] = (byte) depth;
        flags[i] = flag;
        moves[i] = move;
    }
}

    /** Empties the notebook. Call it when starting a new game. */
    public static void clear() {
        java.util.Arrays.fill(keys, 0L);
        java.util.Arrays.fill(scores, 0);
        java.util.Arrays.fill(depths, (byte) 0);
        java.util.Arrays.fill(flags, (byte) 0);
    }
}
