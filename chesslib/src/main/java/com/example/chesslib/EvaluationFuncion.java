package com.example.chesslib;

public class EvaluationFuncion {
    private final MovementManager movementManager = MovementManager.INSTANCE;

    public static final int PAWN_VALUE = 100;
    public static final int BISHOP_VALUE = 320;
    public static final int KNIGHT_VALUE = 300;
    public static final int ROOK_VALUE = 500;
    public static final int QUEEN_VALUE = 900;
    public static final int KING_VALUE = 1000;
    public static final int PST_MULTIPLIER = 1;

    public static final int PASSED_PAWN_BONUS = 20; 
    public static final int ISOLATED_PAWN_PENALTY = -15;
    public static final int ROOK_OPEN_FILE_BONUS = 25;
    public static final int ROOK_SEMI_OPEN_FILE_BONUS = 15;

    private long board = 0L;

    private int whiteScores = 0;
    private int blackScores = 0;

    private int blackAtackSquaresCount = 0;
    private int blackCapureSquaresCount = 0;

    private int whiteAtackSquaresCount = 0;
    private int whiteCapureSquaresCount = 0;

    private final int[] whitePawnsPerFile = new int[8];
    private final int[] blackPawnsPerFile = new int[8];



    public static int Evaluate(){
        return new EvaluationFuncion().evaluate();
    }

    private int evaluate(){

        if (movementManager.won == 1) {
            return 9_000_000;
        }
        if (movementManager.won == 2) {
            return -9_000_000;
        }
        if (movementManager.won == 3 || movementManager.gameFinished) {
            return 0;
        }

        updateBoard();
        whiteScores =0;
        blackScores = 0;
        blackAtackSquaresCount = 0 ;
        blackCapureSquaresCount = 0 ;
        whiteAtackSquaresCount = 0 ;
        whiteCapureSquaresCount = 0 ; 

        java.util.Arrays.fill(whitePawnsPerFile, 0);
        java.util.Arrays.fill(blackPawnsPerFile, 0);
        
        long previousAtackSquares = Board.atackSquares;
        long previousCaptureSquares = Board.captureSquares;
        try {
            for (int i=0; i<64; i++){
                if (((board >>> i) & 1L)==1){
                    int piece = returnPiece(i);
                    Board.atackSquares = 0L;
                    Board.captureSquares = 0L;
                    if (piece < 7) {
                        switch (piece) {
                            case 1 -> {
                                whiteScores += PAWN_VALUE + PAWN_PQT[63 - i] * PST_MULTIPLIER;
                                overlapedPawns(i);
                            }
                            case 2 -> whiteScores += BISHOP_VALUE + BISHOP_PQT[63 - i] * PST_MULTIPLIER;
                            case 3 -> whiteScores += ROOK_VALUE + ROOK_MG_PQT[63 - i] * PST_MULTIPLIER;
                            case 4 -> whiteScores += QUEEN_VALUE + QUEEN_MG_PQT[63 - i] * PST_MULTIPLIER;
                            case 5 -> whiteScores += KNIGHT_VALUE + KNIGHT_PQT[63 - i] * PST_MULTIPLIER;
                            case 6 -> {
                                int phase = getGamePhaseScore();
                                whiteScores += KING_VALUE + getKingScore(i, true, phase) * PST_MULTIPLIER;
                            }
                            default -> { }
                        }

                        generateMovement(piece, i);
                        whiteAtackSquaresCount += Long.bitCount(Board.atackSquares);
                        whiteCapureSquaresCount += Long.bitCount(Board.captureSquares);
                    } else {
                        switch (piece) {
                            case 7 -> {
                                blackScores += PAWN_VALUE + PAWN_PQT[i] * PST_MULTIPLIER;
                                BoverlapedPawns(i);
                            }
                            case 8 -> blackScores += BISHOP_VALUE + BISHOP_PQT[i] * PST_MULTIPLIER;
                            case 9 -> blackScores += ROOK_VALUE + ROOK_MG_PQT[i] * PST_MULTIPLIER;
                            case 10 -> blackScores += QUEEN_VALUE + QUEEN_MG_PQT[i] * PST_MULTIPLIER;
                            case 11 -> blackScores += KNIGHT_VALUE + KNIGHT_PQT[i] * PST_MULTIPLIER;
                            case 12 -> {
                                int phase = getGamePhaseScore();
                                blackScores += KING_VALUE + getKingScore(i, false, phase) * PST_MULTIPLIER;
                            }
                            default -> { }
                        }

                        generateMovement(piece, i);
                        blackAtackSquaresCount += Long.bitCount(Board.atackSquares);
                        blackCapureSquaresCount += Long.bitCount(Board.captureSquares);
                    }
                }
            }
        } finally {
            Board.atackSquares = previousAtackSquares;
            Board.captureSquares = previousCaptureSquares;
        }
        if (movementManager.blackIsInCheck){
            blackScores -= 50;
        }
        if (movementManager.whiteIsInCheck){
            whiteScores -= 50;
        }


        whiteScores += whiteCapureSquaresCount * 4 + whiteAtackSquaresCount * 2;
        blackScores += blackCapureSquaresCount * 4 + blackAtackSquaresCount * 2;

        whiteScores += PawnOverlapScore();

 
        return whiteScores - blackScores + evaluatePawnAndRookStructures()  ;
    }

    private void generateMovement(int piece, int square) {
        switch (piece) {
            case 1 -> movementManager.WPMouvement(square);
            case 2 -> movementManager.WBMouvement(square);
            case 3 -> movementManager.WRMouvement(square);
            case 4 -> movementManager.WQMouvement(square);
            case 5 -> movementManager.WKNMovement(square);
            case 6 -> movementManager.WKMovement(square);
            case 7 -> movementManager.BPMouvement(square);
            case 8 -> movementManager.BBMouvement(square);
            case 9 -> movementManager.BRMouvement(square);
            case 10 -> movementManager.BQMouvement(square);
            case 11 -> movementManager.BKNMovement(square);
            case 12 -> movementManager.BKMovement(square);
            default -> throw new IllegalArgumentException("Unknown piece type: " + piece);
        }
    }

    private void updateBoard(){
        movementManager.updateWhiteBoard();
        movementManager.updateBlackBoard();
        movementManager.updateBoard();
        board =Board.board;
    }

    public static int returnPiece(int i){
        long mask = 1L << i;

        if ((Board.whitePawns & mask) != 0L) return 1;
        if ((Board.whiteBishops & mask) != 0L) return 2;
        if ((Board.whiteKnights & mask) != 0L) return 5;
        if ((Board.whiteRoocks & mask) != 0L) return 3;
        if ((Board.whiteQueen & mask) != 0L) return 4;
        if ((Board.whiteKing & mask) != 0L) return 6;

        if ((Board.blackPawns & mask) != 0L) return 7;
        if ((Board.blackBishops & mask) != 0L) return 8;
        if ((Board.blackKnights & mask) != 0L) return 11;
        if ((Board.blackRoocks & mask) != 0L) return 9;
        if ((Board.blackQueen & mask) != 0L) return 10;
        if ((Board.blackKing & mask) != 0L) return 12;
        
        return 0;
    }

    public static int returnPieceValue(int piece){
        return switch (piece) {
            case 1, 7 -> PAWN_VALUE;
            case 2, 8 -> BISHOP_VALUE;
            case 3, 9 -> ROOK_VALUE;
            case 4, 10 -> QUEEN_VALUE;
            case 5, 11 -> KNIGHT_VALUE;
            case 6, 12 -> KING_VALUE;
            default -> 0;
        };
    }

    public static final int[] PAWN_PQT = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
        99, 99, 99, 99, 99, 99, 99, 99
    };

    // Knight Heat Map: Penalizes edges ("Knights on the rim are dim") and favors the center
    public static final int[] KNIGHT_PQT = {
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20,   0,   0,   0,   0, -20, -40,
        -30,   0,  10,  15,  15,  10,   0, -30,
        -30,   5,  15,  20,  20,  15,   5, -30,
        -30,   0,  15,  20,  20,  15,   0, -30,
        -30,   5,  10,  15,  15,  10,   5, -30,
        -40, -20,   0,   5,   5,   0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    };

    // Bishop Heat Map: Encourages long diagonals and central presence
    public static final int[] BISHOP_PQT = {
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10,   0,   0,   0,   0,   0,   0, -10,
        -10,   0,   5,  10,  10,   5,   0, -10,
        -10,   5,   5,  10,  10,   5,   5, -10,
        -10,   0,  10,  10,  10,  10,   0, -10,
        -10,  10,  10,  10,  10,  10,  10, -10,
        -10,   5,   0,   0,   0,   0,   5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    };

    // King Middle-Game Heat Map: Encourages castling and pawn-shield safety
    public static final int[] KING_MIDDLE_GAME_PQT = {
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
         20,  20,   0,   0,   0,   0,  20,  20,
         40,  50,  10,   0,   0,  10,  50,  40
    };

    public static final int[] ROOK_MG_PQT = {
        32,  42,  32,  51,  63,  9,  31,  43,  // Rank 8
        27,  32,  58,  62,  80, 67,  26,  44,  // Rank 7 (7th rank invasion)
        -5,  19,  26,  36,  17, 45,  61,  16,  // Rank 6
        -24, -11,   7,  26,  24, 35,  -8, -20,  // Rank 5
        -36, -26,  -6,   1,  -2, -4, -15, -40,  // Rank 4
        -45, -25, -16, -17,  -3,  1,  -5, -33,  // Rank 3
        -44, -16, -20,  -9,  -1, 11,  -6, -71,  // Rank 2
        -19, -13,   1,  17,  16,  7, -37, -26   // Rank 1
    };
 
    public static final int[] QUEEN_MG_PQT = {
        -28, -20, -12,  -1,  -1, -12, -20, -28,  // Rank 8
        -18, -13,  -4,   0,   0,  -4, -13, -18,  // Rank 7
        -10,  -8,  -1,   4,   4,  -1,  -8, -10,  // Rank 6
        -9,  -4,   0,   6,   6,   0,  -4,  -9,  // Rank 5
        -10,  -4,   0,   6,   6,   0,  -4, -10,  // Rank 4
        -10,  -1,   6,   4,   4,   6,  -1, -10,  // Rank 3
        -16, -11,   0,   2,   2,   0, -11, -16,  // Rank 2
        -28, -17, -13,  -2,  -2, -13, -17, -28   // Rank 1
    };

        // Add an Endgame King Table for open boards
    public static final int[] KING_END_GAME_PQT = {
        -50,-35,-30,-25,-25,-30,-35,-50,
        -35,-10,-5,  0,  0, -5,-10,-35,
        -30, -5, 10, 15, 15, 10, -5,-30,
        -25,  0, 15, 20, 20, 15,  0,-25,
        -25,  0, 15, 20, 20, 15,  0,-25,
        -30, -5, 10, 15, 15, 10, -5,-30,
        -35,-10,-5,  0,  0, -5,-10,-35,
        -50,-35,-30,-25,-25,-30,-35,-50
    };

    private static final long[] FILE_MASKS = {
        0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L, 0x0808080808080808L,
        0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
    };

    // Adjacent files mask for isolation checks
    private static final long[] ADJACENT_FILES = {
        0x0202020202020202L, 0x0505050505050505L, 0x0A0A0A0A0A0A0A0AL, 0x1414141414141414L,
        0x2828282828282828L, 0x5050505050505050L, 0xA0A0A0A0A0A0A0A0L, 0x4040404040404040L
    };



    private void overlapedPawns(int square){
        whitePawnsPerFile[square % 8]++;
    }

    private void BoverlapedPawns(int square){
        blackPawnsPerFile[square % 8]++;
    }

    private int PawnOverlapScore(){
        int score = 0;
        for (int file = 0; file < 8; file++){
            score += Math.max(0, blackPawnsPerFile[file] - 1) * 25;
            score -= Math.max(0, whitePawnsPerFile[file] - 1) * 25;
        }

        return score;
    }

    private int evaluatePawnAndRookStructures() {
        int score = 0;
        
        // 1. Passed Pawns & Isolation
        for (int file = 0; file < 8; file++) {
            long whiteFilePawns = Board.whitePawns & FILE_MASKS[file];
            long blackFilePawns = Board.blackPawns & FILE_MASKS[file];
            
            // Isolated pawn check (no friendly pawns on adjacent files)
            long adjMask = ADJACENT_FILES[file];
            if (whiteFilePawns != 0L && (Board.whitePawns & adjMask) == 0L) {
                score += ISOLATED_PAWN_PENALTY;
            }
            if (blackFilePawns != 0L && (Board.blackPawns & adjMask) == 0L) {
                score -= ISOLATED_PAWN_PENALTY;
            }
            
            // Passed pawns evaluation
            while (whiteFilePawns != 0L) {
                int sq = Long.numberOfTrailingZeros(whiteFilePawns);
                int rank = sq / 8;
                // Check if any black pawn is ahead on the same or adjacent files
                long obstructionMask = (0xFFFFFFFFFFFFFFFFL << ((rank + 1) * 8)) & adjMask;
                if ((Board.blackPawns & obstructionMask) == 0L) {
                    score += PASSED_PAWN_BONUS * rank; // More valuable closer to promotion
                }
                whiteFilePawns &= whiteFilePawns - 1;
            }
            
            while (blackFilePawns != 0L) {
                int sq = Long.numberOfTrailingZeros(blackFilePawns);
                int rank = sq / 8;
                long obstructionMask = (0xFFFFFFFFFFFFFFFFL >>> (64 - (rank * 8))) & adjMask; // looking down
                if ((Board.whitePawns & obstructionMask) == 0L) {
                    score -= PASSED_PAWN_BONUS * (7 - rank);
                }
                blackFilePawns &= blackFilePawns - 1;
            }
        }
        
        // 2. Rook Open and Semi-Open files
        for (int file = 0; file < 8; file++) {
            boolean whiteHasPawn = (Board.whitePawns & FILE_MASKS[file]) != 0L;
            boolean blackHasPawn = (Board.blackPawns & FILE_MASKS[file]) != 0L;
            
            long whiteRooksOnFile = Board.whiteRoocks & FILE_MASKS[file];
            long blackRooksOnFile = Board.blackRoocks & FILE_MASKS[file];
            
            if (whiteRooksOnFile != 0L) {
                if (!whiteHasPawn && !blackHasPawn) score += ROOK_OPEN_FILE_BONUS;
                else if (!whiteHasPawn) score += ROOK_SEMI_OPEN_FILE_BONUS;
            }
            if (blackRooksOnFile != 0L) {
                if (!whiteHasPawn && !blackHasPawn) score -= ROOK_OPEN_FILE_BONUS;
                else if (!blackHasPawn) score -= ROOK_SEMI_OPEN_FILE_BONUS;
            }
        }
        
        return score;
    }

    private static int getGamePhaseScore() {
        // Count total non-pawn material remaining to determine phase
        //int pawns = Long.bitCount(Board.whitePawns | Board.blackPawns);
        int knights = Long.bitCount(Board.whiteKnights | Board.blackKnights);
        int bishops = Long.bitCount(Board.whiteBishops | Board.blackBishops);
        int rooks = Long.bitCount(Board.whiteRoocks | Board.blackRoocks);
        int queens = Long.bitCount(Board.whiteQueen | Board.blackQueen);
        
        // Total material weight (Max phase weight = 24)
        int phase = knights * 1 + bishops * 1 + rooks * 2 + queens * 4;
        return Math.min(24, phase); // 24 = full middle game, 0 = pure endgame
    }

    // Inside your king evaluation block, interpolate the PST values:
    private int getKingScore(int sq, boolean isWhite, int phase) {
        int mgScore = KING_MIDDLE_GAME_PQT[isWhite ? 63 - sq : sq];
        int egScore = KING_END_GAME_PQT[isWhite ? 63 - sq : sq];
        
        // Linearly interpolate between Middle Game (phase 24) and Endgame (phase 0)
        return (mgScore * phase + egScore * (24 - phase)) / 24;
    }

}
