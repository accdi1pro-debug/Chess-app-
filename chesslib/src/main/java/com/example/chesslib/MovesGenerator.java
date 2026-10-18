package com.example.chesslib;

import java.util.ArrayList;
import java.util.List;

public class MovesGenerator {

    public static long TIME_LIMIT_MS = 4000;
    public static volatile boolean isSearching = false;
    private static final int MAX_DEPTH = 30;
    private static long deadline;
    private static volatile boolean stopRequested = false;
    private static long nodes;
    public static int lastCompletedDepth = 0;

    // Add fields in MovesGenerator.java
private static final Move[][] killerMoves = new Move[MAX_DEPTH][2];
private static final int[][] historyTable = new int[13][64];

// Call this when a quiet move causes a beta cutoff:
private static void storeKiller(Move move, int depth) {
    if (depth >= 0 && depth < MAX_DEPTH) {
        if (!move.equals(killerMoves[depth][0])) {
            killerMoves[depth][1] = killerMoves[depth][0];
            killerMoves[depth][0] = move;
        }
    }
    historyTable[move.piece()][move.to()] += depth * depth;
}

// Custom sort method combining MVV-LVA, Killers, and History:
private static int scoreMove(Move m, int depth) {
    int victim = EvaluationFuncion.returnPieceValue(MovementManager.INSTANCE.returnPiece(m.to()));
    int attacker = EvaluationFuncion.returnPieceValue(m.piece());
    
    // 1. Captures first (MVV-LVA)
    if (victim > 0) {
        return 10000 + (victim * 10 - attacker);
    }
    
    // 2. Killer moves second
    if (depth >= 0 && depth < MAX_DEPTH) {
        if (m.equals(killerMoves[depth][0])) return 5000;
        if (m.equals(killerMoves[depth][1])) return 4000;
    }
    
    // 3. History heuristic for remaining quiet moves
    return historyTable[m.piece()][m.to()];
}

    private static final class TimeUp extends RuntimeException {
        TimeUp() { super(null, null, false, false); }
    }

    public static void requestStop() { stopRequested = true; }

    private static boolean timeIsUp() {
        return stopRequested || System.nanoTime() - deadline >= 0;
    }

    public static List<Move> generateBlackMoves(){
        List<Move> moves = new ArrayList<>();

        MovementManager.INSTANCE.updateBlackBoard();

        for (int i =0 ;i<64;i++){
            if (((Board.blackBoard >>> i) & 1L) != 0){

                
                Board.atackSquares = 0;
                Board.captureSquares = 0;
                int pieceType = MovementManager.INSTANCE.returnPiece(i);
                switch (pieceType) {
                    case 7 -> MovementManager.INSTANCE.BPMouvement(i);
                    case 8 -> MovementManager.INSTANCE.BBMouvement(i);
                    case 9 -> MovementManager.INSTANCE.BRMouvement(i);
                    case 10 -> MovementManager.INSTANCE.BQMouvement(i);
                    case 11 -> MovementManager.INSTANCE.BKNMovement(i);
                    case 12 -> MovementManager.INSTANCE.BKMovement(i);
                    default -> { }
                }

                if (Board.atackSquares ==0){
                    continue;
                }

                for (int r = 0; r < 64; r++) {
                    if (((Board.atackSquares >>> r) & 1L) != 0) {
                        if (((Board.captureSquares >>> r) & 1L) != 0){
                            moves.add(0,new Move(i, r, pieceType));
                        }else{
                            moves.add(new Move(i, r, pieceType));
                        }
                    }
                }

            }
        }
        return moves;
        
    }

    public static List<Move> generateWhiteMoves(){
        List<Move> moves = new ArrayList<>();

        MovementManager.INSTANCE.updateWhiteBoard();
        for (int i = 0 ; i<64 ; i++){
            Board.atackSquares =0;
            if (((Board.whiteBoard >>> i)& 1L) ==1){
                int piece = MovementManager.INSTANCE.returnPiece(i);
                switch (piece) {
                    case 1 -> MovementManager.INSTANCE.WPMouvement(i);
                    case 2 -> MovementManager.INSTANCE.WBMouvement(i);
                    case 3 -> MovementManager.INSTANCE.WRMouvement(i);
                    case 4 -> MovementManager.INSTANCE.WQMouvement(i);
                    case 5 -> MovementManager.INSTANCE.WKNMovement(i);
                    case 6 -> MovementManager.INSTANCE.WKMovement(i);
                    default -> { }
                }
                
                if (Board.atackSquares != 0) {

                    for (int r = 0; r < 64; r++) {
                        if (((Board.atackSquares >>> r) & 1L) != 0) {
                            if (((Board.captureSquares >>> r) & 1L) != 0){
                                moves.add(0,new Move(i, r, piece));

                            }else{
                                moves.add(new Move(i, r, piece));
                                
                            }
                        }
                        
                    }

                }
            }
        }
        return moves;
        
    }

    
    public static int shearch(int depth, int alpha, int beta) {
        nodes++;
        if ((nodes & 1023) == 0 && timeIsUp()) throw new TimeUp();
        if (MovementManager.INSTANCE.gameFinished) return EvaluationFuncion.Evaluate();

        long key = Zobrist.hash();
        int alphaOrig = alpha;
        int betaOrig = beta;

        // TT LOOKUP (Hash Cut)
        if (TranspositionTable.has(key, depth)) {
            int stored = TranspositionTable.score(key);
            byte flag = TranspositionTable.flag(key);
            if (flag == TranspositionTable.EXACT) return stored;
            if (flag == TranspositionTable.LOWER && stored >= beta) return stored;
            if (flag == TranspositionTable.UPPER && stored <= alpha) return stored;
        }

        if (depth == 0) return quiesce(alpha, beta, 0);

        boolean maximizing = MovementManager.INSTANCE.turn;
        List<Move> candidates = maximizing ? generateWhiteMoves() : generateBlackMoves();
        if (candidates.isEmpty()) return EvaluationFuncion.Evaluate();

        // Sort using your enhanced move scoring
        candidates.sort((a, b) -> scoreMove(b, depth) - scoreMove(a, depth));

        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Move bestMove = null;

        for (Move move : candidates) {
            Board.State boardState = Board.captureState();
            MovementManager.State managerState = MovementManager.INSTANCE.captureState();
            int score;
            try {
                Move(move);
                if (MovementManager.INSTANCE.turn == maximizing) {
                    continue;
                }
                score = shearch(depth - 1, alpha, beta);
            } finally {
                MovementManager.INSTANCE.resetState(managerState);
                Board.resetState(boardState);
            }

            if (maximizing) {
                if (score > best) { best = score; bestMove = move; }
                alpha = Math.max(alpha, best);
            } else {
                if (score < best) { best = score; bestMove = move; }
                beta = Math.min(beta, best);
            }
            if (alpha >= beta) {
                if (MovementManager.INSTANCE.returnPiece(move.to()) == 0) {
                    storeKiller(move, depth);
                }
                break;
            }
        }

        // STORE IN TT WITH BEST MOVE
        byte flag;
        if (best <= alphaOrig) flag = TranspositionTable.UPPER;
        else if (best >= betaOrig) flag = TranspositionTable.LOWER;
        else flag = TranspositionTable.EXACT;
        
        TranspositionTable.store(key, depth, best, flag, bestMove);

        MovementManager mm = MovementManager.INSTANCE;
        boolean inCheck = maximizing ? mm.whiteIsInCheck : mm.blackIsInCheck;

        // NULL-MOVE PRUNING
        int R = 2; // Reduction depth
        if (!inCheck && depth >= 3 && best != Integer.MIN_VALUE && best != Integer.MAX_VALUE) {
            // Make a null move by toggling the turn
            Board.State bs = Board.captureState();
            MovementManager.State ms = mm.captureState();
            
            mm.turn = !mm.turn;
            int nullScore;
            try {
                nullScore = -shearch(depth - 1 - R, -beta, -beta + 1);
            } finally {
                mm.resetState(ms);
                Board.resetState(bs);
            }
            
            if (nullScore >= beta) {
                return beta; // Beta cutoff via null move!
            }
        }

        return best;
    }

        /**
     * Quiescence search: at the end of the normal search, keep searching captures
     * (and promotions) until the position is "quiet", so the engine never evaluates
     * in the middle of an exchange.
     */
        private static final int MAX_QDEPTH = 8;      // max plies of quiescence
    private static final int MAX_QCHECK_DEPTH = 2; // only search check escapes near the leaf

    private static int quiesce(int alpha, int beta, int qdepth) {
        nodes++;
        if ((nodes & 1023) == 0 && timeIsUp()) {
            throw new TimeUp();
        }

        MovementManager mm = MovementManager.INSTANCE;
        if (mm.gameFinished) {
            return EvaluationFuncion.Evaluate();
        }

        // safety net: never go deeper than this
        if (qdepth >= MAX_QDEPTH) {
            return EvaluationFuncion.Evaluate();
        }

        boolean maximizing = mm.turn;
        boolean inCheck = maximizing ? mm.whiteIsInCheck : mm.blackIsInCheck;

        int best;
        List<Move> moves;

        if (inCheck && qdepth < MAX_QCHECK_DEPTH) {
            // In check: search every escape (only close to the leaf, to avoid endless check chains)
            moves = maximizing ? generateWhiteMoves() : generateBlackMoves();
            if (moves.isEmpty()) {
                return EvaluationFuncion.Evaluate();
            }
            best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        } else {
            int standPat = EvaluationFuncion.Evaluate();
            if (maximizing) {
                if (standPat >= beta) return standPat;
                alpha = Math.max(alpha, standPat);
            } else {
                if (standPat <= alpha) return standPat;
                beta = Math.min(beta, standPat);
            }
            best = standPat;
            moves = generateCaptures(maximizing);
        }

        boolean searchedAny = false;

        for (Move move : moves) {
            Board.State boardState = Board.captureState();
            MovementManager.State managerState = mm.captureState();
            int score;
            try {
                Move(move);
                // guard: if the side to move didn't change, the move failed -> skip it
                if (mm.turn == maximizing) {
                    continue;   // the finally block still restores the position
                }
                score = quiesce(alpha, beta, qdepth + 1);
            } finally {
                mm.resetState(managerState);
                Board.resetState(boardState);
            }

            searchedAny = true;
            if (maximizing) {
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, score);
                beta = Math.min(beta, best);
            }
            if (alpha >= beta) {
                break;
            }
        }

        // in-check branch where every move was skipped: fall back to a static evaluation
        if (!searchedAny && (best == Integer.MIN_VALUE || best == Integer.MAX_VALUE)) {
            return EvaluationFuncion.Evaluate();
        }
        return best;
    }

    /** Only captures and promotions, best captures first (MVV-LVA). */
    public static List<Move> generateCaptures(boolean white) {
        MovementManager mm = MovementManager.INSTANCE;
        mm.updateWhiteBoard();
        mm.updateBlackBoard();
        mm.updateBoard();

        long own = white ? Board.whiteBoard : Board.blackBoard;
        List<Move> moves = new ArrayList<>();

        for (long bb = own; bb != 0L; bb &= bb - 1) {
            int from = Long.numberOfTrailingZeros(bb);
            int piece = mm.returnPiece(from);

            Board.atackSquares = 0L;
            Board.captureSquares = 0L;
            generatePieceMoves(piece, from);

            // & atackSquares: makes sure pinned pieces only keep their legal captures
            long targets = Board.captureSquares & Board.atackSquares;
            if (piece == 1) targets |= Board.atackSquares & Board.EighthRow; // white promotion
            if (piece == 7) targets |= Board.atackSquares & Board.FirstRow;  // black promotion

            for (long t = targets; t != 0L; t &= t - 1) {
                moves.add(new Move(from, Long.numberOfTrailingZeros(t), piece));
            }
        }

        Board.atackSquares = 0L;
        Board.captureSquares = 0L;

        moves.sort((a, b) -> mvvLva(b) - mvvLva(a));
        return moves;
    }

    /** Move score: big victim + small attacker = best (pawn takes queen before queen takes pawn). */
    private static int mvvLva(Move m) {
        int victim = EvaluationFuncion.returnPieceValue(MovementManager.INSTANCE.returnPiece(m.to()));
        int attacker = EvaluationFuncion.returnPieceValue(m.piece());
        int score = victim * 10 - attacker;
        boolean promotion = (m.piece() == 1 && m.to() >= 56) || (m.piece() == 7 && m.to() < 8);
        if (promotion) score += EvaluationFuncion.QUEEN_VALUE * 10;
        return score;
    }

    private static void generatePieceMoves(int piece, int square) {
        MovementManager mm = MovementManager.INSTANCE;
        switch (piece) {
            case 1 -> mm.WPMouvement(square);
            case 2 -> mm.WBMouvement(square);
            case 3 -> mm.WRMouvement(square);
            case 4 -> mm.WQMouvement(square);
            case 5 -> mm.WKNMovement(square);
            case 6 -> mm.WKMovement(square);
            case 7 -> mm.BPMouvement(square);
            case 8 -> mm.BBMouvement(square);
            case 9 -> mm.BRMouvement(square);
            case 10 -> mm.BQMouvement(square);
            case 11 -> mm.BKNMovement(square);
            case 12 -> mm.BKMovement(square);
            default -> { }
        }
    }
    public static void play() {
        Move move = getBestMove();
        if (move != null) Move(move);
    }

    public static Move getBestMove() {
        boolean wasSearching = isSearching;
        isSearching = true;
        try {
            return findBestMove(TIME_LIMIT_MS);
        } finally {
            isSearching = wasSearching;
        }
    }

    public static Move findBestMove(long timeLimitMs) {
        boolean wasSearching = isSearching;
        isSearching = true;
        try {
            deadline = System.nanoTime() + timeLimitMs * 1_000_000L;
            stopRequested = false;
            nodes = 0;

            boolean maximizing = MovementManager.INSTANCE.turn;
            List<Move> candidates = maximizing ? generateWhiteMoves() : generateBlackMoves();
            if (candidates.isEmpty()) return null;

            Move bestSoFar = candidates.get(0);
            int bestScore = 0;

            for (int depth = 1; depth <= MAX_DEPTH; depth++) {
                try {
                    int alpha = Integer.MIN_VALUE;
                    int beta = Integer.MAX_VALUE;
                    
                    // Apply aspiration windows for depths >= 4
                    if (depth >= 4) {
                        int window = 50; // 50 centipawns window
                        alpha = bestScore - window;
                        beta = bestScore + window;
                    }
                    
                    int score = searchRootWithWindow(candidates, depth, maximizing, alpha, beta);
                    
                    // If score fell outside the window, re-search with full bounds
                    if (depth >= 4 && (score <= alpha || score >= beta)) {
                        alpha = Integer.MIN_VALUE;
                        beta = Integer.MAX_VALUE;
                        score = searchRootWithWindow(candidates, depth, maximizing, alpha, beta);
                    }
                    
                    bestScore = score;
                    bestSoFar = candidates.get(0);
                    lastCompletedDepth = depth;
                    
                } catch (TimeUp e) {
                    break;
                }
                if (timeIsUp()) break;
            }
            return bestSoFar;
        } finally {
            isSearching = wasSearching;
        }
    }

    private static int searchRootWithWindow(List<Move> candidates, int depth, boolean maximizing, int alpha, int beta) {
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Move bestMove = null;
        
        for (int i = 0; i < candidates.size(); i++) {
            Move move = candidates.get(i);
            Board.State bs = Board.captureState();
            MovementManager.State ms = MovementManager.INSTANCE.captureState();
            int score;
            try {
                Move(move);
                if (MovementManager.INSTANCE.turn == maximizing) {
                    continue;
                }
                score = shearch(depth - 1, alpha, beta);
            } finally {
                MovementManager.INSTANCE.resetState(ms);
                Board.resetState(bs);
            }
            
            if (maximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (maximizing) alpha = Math.max(alpha, bestScore);
            else beta = Math.min(beta, bestScore);
        }
        if (bestMove != null) {
            candidates.remove(bestMove);
            candidates.add(0, bestMove);
        }
        return bestScore;
    }
    
    public static void saveState(){
        MovementManager.INSTANCE.saveState();
        Board.saveState();
    }

    public static void resetState(MovementManager.State mmState, Board.State boardState){
        MovementManager.INSTANCE.resetState(mmState);
        Board.resetState(boardState);
    }

    public static void resetState(){
        MovementManager.INSTANCE.resetState();
        Board.resetState();
    }

    public static void Move(Move move){

        MovementManager.INSTANCE.showAtackSquare(move.piece(),move.from());
        MovementManager.INSTANCE.movePiece(move.to());

    }


}