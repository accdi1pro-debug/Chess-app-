package com.example.chesslib;

public class MovementManager {
    public static final MovementManager INSTANCE = new MovementManager();

    public  int pieceMoved = 0;
    public  int pieceMovedPos = 0;

    public  int SpieceMoved = 0;
    public  int SpieceMovedPos = 0;
    public  boolean isCapture = false;
    public  int pieceCapuredSquare = 0;
    public  int pieceCapured = 0;

    public  boolean WhiteKingHasBeenMoved = false;
    public  boolean blackKingHasBeenMoved = false;

    public  boolean WhiteKingHasBeenChecked = false;
    public  boolean blackKingHasBeenChecked = false;

    public  int whiteKingPos = 3;
    public  int blackKingPos = 59;

    public  boolean whiteIsInCheck = false ;
    public  boolean blackIsInCheck = false ;

    public  boolean turn = true ;

    public  boolean gameFinished = false ;

    public  boolean whitePromotionUI = false;
    public  boolean blackPromotionUI = false;

    public  int promotionPiece = 0;
    public  int promotionsquare = 0;

    public  boolean WhiteLeftRookHasBeenMoved = false;
    public  boolean WhiteRightRookHasBeenMoved = false;

    public  boolean BlackLeftRookHasBeenMoved = false;
    public  boolean BlackRightRookHasBeenMoved = false;

    public  boolean BlackAi = true ;
    public  boolean whiteAi = false ;

    public  int won =0;

    public  int count = 0;

    public  void showAtackSquare(int pieceType , int arayPos){
        if (gameFinished){
            return;
        }
        Board.atackSquares = 0;
        pieceMoved = pieceType;
        pieceMovedPos = arayPos;
        Board.captureSquares = 0;
        
        if (turn == true){
            switch (pieceType) {
                case 1 -> WPMouvement(arayPos);
                case 2 -> WBMouvement(arayPos);
                case 3 -> WRMouvement(arayPos);
                case 4 -> WQMouvement(arayPos);
                case 5 -> WKNMovement(arayPos);
                case 6 -> WKMovement(arayPos);
                default -> { }
            }
        } else {
            switch (pieceType) {
                case 7 -> BPMouvement(arayPos);
                case 8 -> BBMouvement(arayPos);
                case 9 -> BRMouvement(arayPos);
                case 10 -> BQMouvement(arayPos);
                case 11 -> BKNMovement(arayPos);
                case 12 -> BKMovement(arayPos);
                default -> { }
            }
        }

    }
    
    public  void movePiece(int arayPos ){
        Board.move = 0L;

        if (((Board.atackSquares >>> arayPos) & 1L) == 0){
            return;
        }
        isCapture = false;
        pieceCapured = 0;
        pieceCapuredSquare = -1;
        long curentBoard = 0 ;

        if (turn == true){
            switch (pieceMoved) {
                case 1 -> curentBoard = Board.whitePawns ;
                case 2 -> curentBoard = Board.whiteBishops ;
                case 3 -> {
                    if (pieceMovedPos == 0){
                        WhiteRightRookHasBeenMoved=true;
                    }
                    if (pieceMovedPos == 7){
                        WhiteLeftRookHasBeenMoved=true;
                    }
                    curentBoard = Board.whiteRoocks ;
                }
                case 4 -> curentBoard = Board.whiteQueen ;
                case 5 -> curentBoard = Board.whiteKnights ;
                case 6 -> {
                    // Castling = the only king move of 2 squares. The move generator only offers it
                    // when it is legal, so here we just move the rook too.
                    if (pieceMovedPos == 3 && arayPos == 1 && (Board.whiteRoocks & 1L) != 0) {          // O-O: rook h1 -> f1
                        Board.whiteRoocks &= ~1L;
                        Board.whiteRoocks |= (1L << 2);
                        WhiteRightRookHasBeenMoved = true;
                    }
                    if (pieceMovedPos == 3 && arayPos == 5 && (Board.whiteRoocks & (1L << 7)) != 0) {   // O-O-O: rook a1 -> d1
                        Board.whiteRoocks &= ~(1L << 7);
                        Board.whiteRoocks |= (1L << 4);
                        WhiteLeftRookHasBeenMoved = true;
                    }
                    whiteKingPos = arayPos;
                    WhiteKingHasBeenMoved = true;
                    curentBoard = Board.whiteKing ; 
                }
                default -> { }
            }
        }else{
            switch (pieceMoved) {
                case 7 -> curentBoard = Board.blackPawns ;
                case 8 -> curentBoard = Board.blackBishops ;
                case 9 -> {
                    if (pieceMovedPos == 56){
                        BlackRightRookHasBeenMoved=true;
                    }
                    if (pieceMovedPos == 63){
                        BlackLeftRookHasBeenMoved=true;
                    }
                    curentBoard = Board.blackRoocks ;
                }
                case 10 -> curentBoard = Board.blackQueen ;
                case 11 -> curentBoard = Board.blackKnights ;
                case 12 -> {
                    if (pieceMovedPos == 59 && arayPos == 57 && (Board.blackRoocks & (1L << 56)) != 0) {  // O-O: rook h8 -> f8
                        Board.blackRoocks &= ~(1L << 56);
                        Board.blackRoocks |= (1L << 58);
                        BlackRightRookHasBeenMoved = true;
                    }
                    if (pieceMovedPos == 59 && arayPos == 61 && (Board.blackRoocks & (1L << 63)) != 0) {  // O-O-O: rook a8 -> d8
                        Board.blackRoocks &= ~(1L << 63);
                        Board.blackRoocks |= (1L << 60);
                        BlackLeftRookHasBeenMoved = true;
                    }
                    blackKingHasBeenMoved = true;
                    blackKingPos = arayPos;
                    curentBoard = Board.blackKing ;
                }
                default -> { }
            }
        }

        if (pieceMoved == 1 ||pieceMoved ==7){
            count =0;
        }else{
            count++;
        }
        

        // A rook captured on its starting corner can no longer castle.
        if (arayPos == 0)  WhiteRightRookHasBeenMoved = true;
        if (arayPos == 7)  WhiteLeftRookHasBeenMoved = true;
        if (arayPos == 56) BlackRightRookHasBeenMoved = true;
        if (arayPos == 63) BlackLeftRookHasBeenMoved = true;

        if (((Board.captureSquares >>> arayPos) & 1L) != 0) {
            pieceCapuredSquare =arayPos;
            isCapture = true;
            count =0;
            removePiece(arayPos);
        }

        curentBoard ^= (1L << pieceMovedPos) ;

        curentBoard ^= (1L << arayPos) ;


        if (pieceMoved == 1 && arayPos >= 56 ){
            if (whiteAi || MovesGenerator.isSearching){
                curentBoard ^= (1L << arayPos);          // remove the pawn
                Board.whiteQueen |= (1L << arayPos);     // add a queen
            }else{
                whitePromotionUI = true;
                promotionsquare = arayPos;
                Board.whitePawns = curentBoard;
                return;
            }
        }
        if (pieceMoved == 7 && arayPos < 8 ){
            if (BlackAi || MovesGenerator.isSearching){
                curentBoard ^= (1L << arayPos);          // remove the pawn
                Board.blackQueen |= (1L << arayPos);     // add a queen
            }else{
                blackPromotionUI = true;
                promotionsquare = arayPos;
                Board.blackPawns = curentBoard;
                return;
            }
        }

        // Update the board with the moved piece
        switch (pieceMoved) {
            case 1 -> Board.whitePawns = curentBoard;
            case 2 -> Board.whiteBishops = curentBoard;
            case 3 -> Board.whiteRoocks = curentBoard;
            case 4 -> Board.whiteQueen = curentBoard;
            case 5 -> Board.whiteKnights = curentBoard;
            case 6 -> Board.whiteKing = curentBoard;
            case 7 -> Board.blackPawns = curentBoard;
            case 8 -> Board.blackBishops =curentBoard  ;
            case 9 -> Board.blackRoocks=curentBoard   ;
            case 10 -> Board.blackQueen  =curentBoard ;
            case 11 -> Board.blackKnights  =curentBoard ;
            case 12 -> Board.blackKing =curentBoard  ;
            default -> { }
        }
        finishMove(arayPos);
    }
    

    /**
     * Second half of a move: refresh boards, detect check / mate / stalemate / 50-move,
     * switch the side to move. Shared by normal moves and human promotions.
     */
    private void finishMove(int arayPos) {
        Board.atackSquares = 0;
        Board.captureSquares = 0;

        Board.checkSquares = 0;
        Board.kingCheckSquares = 0;
        updateWhiteBoard();
        updateBlackBoard();
        updateBoard();

        if (turn == true){
            checkBlackCheck(blackKingPos);
            //System.out.println("black is in check");
            
            if (blackIsInCheck){
                //System.out.println(" BC true");
                blackKingHasBeenChecked = true;
            }
            WhiteAtackSquares();
            
        }else{
            checkWhiteCheck(whiteKingPos);
            //System.out.println("whte is in check");
            if (whiteIsInCheck){
                WhiteKingHasBeenChecked = true;
                //System.out.println(" WC true");
            }
            BlackAtackSquares();
            
        }

        if (!turn){
            if (checkWhiteMateCheck()){
                gameFinished = true;

                if (whiteIsInCheck){

                    won = 2;
                }else{
                    won =3;

                }
            }
        }else{
            if (checkBlackMateCheck()){
                gameFinished = true;

                if (blackIsInCheck){

                    won = 1;
                }else{
                    won =3;

                }
            }
        }

        if (count >= 50){
            gameFinished = true;
            won =3;

        }

        

        turn = !turn ;
        
        SpieceMoved =pieceMoved;
        SpieceMovedPos=arayPos;

        
        updateBlackBoard();
        updateWhiteBoard();
        updateBoard();
        Board.move = (1L << arayPos) | (1L << pieceMovedPos);

        Board.atackSquares = 0;
        Board.captureSquares = 0;
        pieceMoved = 0;
        pieceMovedPos = 0;

        
    }

    public boolean isPromotionPending() {
        return whitePromotionUI || blackPromotionUI;
    }

    //white pieces movement 

    public void WPMouvement(int arayPos) {
        long pawnBB = 1L << arayPos;
        long empty = ~Board.board;

        // 1. Captures
        Board.captureSquares = Board.blackBoard & MagicBitBoards.PAWN_ATTACKS[0][arayPos];

        long singlePush = (pawnBB << 8) & empty;

        long doublePush = 0L;
        if (singlePush != 0L && (arayPos >= 8 && arayPos <= 15)) { // Rank 2 check
            doublePush = (pawnBB << 16) & empty;
        }

        if (whiteIsInCheck) {
            long checkMask = Board.kingCheckSquares;
            Board.captureSquares &= checkMask;
            singlePush &= checkMask;
            doublePush &= checkMask;
        }

        if ((Board.checkSquares & (1L << arayPos)) == 0) {
            Board.atackSquares = singlePush | doublePush | Board.captureSquares;
        } else {
            Board.atackSquares = (singlePush | doublePush | Board.captureSquares) & Board.checkSquares;
        }

    }

    public void WBMouvement(int arayPos) {

        long BB = MagicBitBoards.getBishopAttacks(arayPos, Board.board);
        if (!whiteIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) != 0) {
                Board.atackSquares =( BB & ~Board.whiteBoard  )& Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.blackBoard) & Board.checkSquares;
            } else {
                Board.atackSquares = BB & ~Board.whiteBoard;
                Board.captureSquares = Board.atackSquares & Board.blackBoard;
            }

        } else {

            Board.atackSquares = (BB & ~Board.whiteBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.blackBoard) & Board.kingCheckSquares;
        }

    }

    public void WRMouvement(int arayPos) {
        long RR = MagicBitBoards.getRookAttacks(arayPos, Board.board);
        if (!whiteIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) != 0) {
                Board.atackSquares = (RR & ~Board.whiteBoard)& Board.checkSquares;
                Board.captureSquares =( Board.atackSquares & Board.blackBoard)& Board.checkSquares;
            } else {
                Board.atackSquares = RR & ~Board.whiteBoard;
                Board.captureSquares = Board.atackSquares & Board.blackBoard;
            }

        } else {

            Board.atackSquares = (RR & ~Board.whiteBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.blackBoard) & Board.kingCheckSquares;
        }

    }

    public void WQMouvement(int arayPos) {
        long RR = MagicBitBoards.getQueenAttacks(arayPos, Board.board);
        if (!whiteIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) != 0) {
                Board.atackSquares = (RR & ~Board.whiteBoard) & Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.blackBoard )& Board.checkSquares;
            } else {
                Board.atackSquares = RR & ~Board.whiteBoard;
                Board.captureSquares = Board.atackSquares & Board.blackBoard;
            }

        } else {

            Board.atackSquares = (RR & ~Board.whiteBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.blackBoard) & Board.kingCheckSquares;
        }
    }

    public void WKNMovement(int arayPos) {
        if (!whiteIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) != 0) {
                Board.atackSquares = (MagicBitBoards.KNIGHT_ATTACKS[arayPos] & ~Board.whiteBoard)& Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.blackBoard)& Board.checkSquares;
            } else {
                Board.atackSquares = MagicBitBoards.KNIGHT_ATTACKS[arayPos] & ~Board.whiteBoard;
                Board.captureSquares = Board.atackSquares & Board.blackBoard;
            }

        } else {
            Board.atackSquares = (MagicBitBoards.KNIGHT_ATTACKS[arayPos] & ~Board.whiteBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.blackBoard) & Board.kingCheckSquares;
        }

    }

    public void WKMovement(int arayPos) {

        BlackAtackSquares();

        Board.atackSquares = MagicBitBoards.KING_ATTACKS[arayPos] & ~Board.whiteBoard & ~Board.BlackAtackSquares;
        Board.captureSquares = Board.atackSquares & Board.blackBoard;
        checkWhiteCastling();

    }

    //Black pieces movement

    public void BPMouvement(int arayPos) {
        long pawnBB = 1L << arayPos;
        long empty = ~Board.board;

        // 1. Captures
        Board.captureSquares = Board.whiteBoard & MagicBitBoards.PAWN_ATTACKS[1][arayPos];

        long singlePush = (pawnBB >>> 8) & empty;

        long doublePush = 0L;
        if (singlePush != 0L && (arayPos >= 48 && arayPos <= 55)) { // Rank 7 check
            doublePush = (pawnBB >>> 16) & empty;
        }

        if (blackIsInCheck) {
            long checkMask = Board.kingCheckSquares;
            Board.captureSquares &= checkMask;
            singlePush &= checkMask;
            doublePush &= checkMask;
        }
        if ((Board.checkSquares & (1L << arayPos)) == 0) {
            Board.atackSquares = singlePush | doublePush | Board.captureSquares;
            
        } else {
            Board.captureSquares = Board.whiteBoard & MagicBitBoards.PAWN_ATTACKS[1][arayPos]& Board.checkSquares;
            Board.atackSquares = (singlePush | doublePush | Board.captureSquares) & Board.checkSquares;
        }

    }

    public void BBMouvement(int arayPos) {
        long BB = MagicBitBoards.getBishopAttacks(arayPos, Board.board);
        if (!blackIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) != 0) {
                Board.atackSquares = (BB & ~Board.blackBoard)& Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.whiteBoard)& Board.checkSquares;
            } else {
                Board.atackSquares = BB & ~Board.blackBoard;
                Board.captureSquares = Board.atackSquares & Board.whiteBoard;
            }

        } else {

            Board.atackSquares = (BB & ~Board.blackBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.whiteBoard) & Board.kingCheckSquares;
        }
    }

    public void BRMouvement(int arayPos) {
        long RR = MagicBitBoards.getRookAttacks(arayPos, Board.board);
        if (!blackIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) == 0) {
                Board.atackSquares = RR & ~Board.blackBoard;
                Board.captureSquares = Board.atackSquares & Board.whiteBoard;
            } else {
                Board.atackSquares = (RR & ~Board.blackBoard)& Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.whiteBoard)& Board.checkSquares;
            }

        } else {

            Board.atackSquares = (RR & ~Board.blackBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.whiteBoard) & Board.kingCheckSquares;
        }
    }

    public void BQMouvement(int arayPos) {

        long RR = MagicBitBoards.getQueenAttacks(arayPos, Board.board);
        if (!blackIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) == 0) {
                Board.atackSquares = RR & ~Board.blackBoard;
                Board.captureSquares = Board.atackSquares & Board.whiteBoard;
            } else {
                Board.atackSquares = (RR & ~Board.blackBoard)& Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.whiteBoard)& Board.checkSquares;
            }

        } else {

            Board.atackSquares = (RR & ~Board.blackBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.whiteBoard) & Board.kingCheckSquares;
        }

    }

    public void BKNMovement(int arayPos) {
        if (!blackIsInCheck) {
            if ((Board.checkSquares & (1L << arayPos)) == 0) {
                Board.atackSquares = MagicBitBoards.KNIGHT_ATTACKS[arayPos] & ~Board.blackBoard;
                Board.captureSquares = Board.atackSquares & Board.whiteBoard;
            } else {
                Board.atackSquares = (MagicBitBoards.KNIGHT_ATTACKS[arayPos] & ~Board.blackBoard)& Board.checkSquares;
                Board.captureSquares = (Board.atackSquares & Board.whiteBoard)& Board.checkSquares;
            }

        } else {

            Board.atackSquares = (MagicBitBoards.KNIGHT_ATTACKS[arayPos] & ~Board.blackBoard) & Board.kingCheckSquares;
            Board.captureSquares = (Board.atackSquares & Board.whiteBoard) & Board.kingCheckSquares;
        }
    }

    public  void BKMovement(int arayPos){
        WhiteAtackSquares();
        Board.atackSquares = MagicBitBoards.KING_ATTACKS[arayPos] & ~ Board.blackBoard & ~ Board.whiteAtackSquares ;
        Board.captureSquares = Board.atackSquares & Board.whiteBoard;
        checkBlackCastling();
        
    }

    //helpers ustensils

    public  int returnPiece(int i ){

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

    public  boolean checkIfSquareisUsedByEnemey(int i){
        return returnPiece(i) > 6;
    }

    public  boolean checkIfSquareisUsedByEnemeyBlack(int i){
        
        return returnPiece(i) < 7;
    }

    public  boolean checkIfSquareisUsed(int i){
        return returnPiece(i) !=0;
    }

    public  void removePiece(int i){
        if (((Board.whitePawns >>> i) & 1L) != 0) {
            pieceCapured = 1;
            Board.whitePawns ^= (1L << i) ;
        }
        if (((Board.whiteBishops >>> i) & 1L) != 0) {
            pieceCapured = 2;
            Board.whiteBishops ^= (1L << i) ;
        }
        if (((Board.whiteRoocks >>> i) & 1L) != 0) {
            pieceCapured = 3;
            Board.whiteRoocks ^= (1L << i) ;
        }
        if (((Board.whiteKnights >>> i) & 1L) != 0) {
            pieceCapured = 5;
            Board.whiteKnights ^= (1L << i) ;
        }
        if (((Board.whiteQueen >>> i) & 1L) != 0) {
            pieceCapured = 4;
            Board.whiteQueen ^= (1L << i) ;
        }
    

        if (((Board.blackPawns >>> i) & 1L) != 0) {
            pieceCapured = 7;
            Board.blackPawns ^= (1L << i) ;
        }
        if (((Board.blackBishops >>> i) & 1L) != 0) {
            Board.blackBishops ^= (1L << i) ;
            pieceCapured = 8;
        }
        if (((Board.blackRoocks >>> i) & 1L) != 0) {
            pieceCapured = 9;
            Board.blackRoocks ^= (1L << i) ;
        }
        if (((Board.blackKnights >>> i) & 1L) != 0) {
            pieceCapured = 11;
            Board.blackKnights ^= (1L << i) ;
        }
        if (((Board.blackQueen >>> i) & 1L) != 0) {
            pieceCapured = 10;
            Board.blackQueen ^= (1L << i) ;
        }
        
    }

    // Check handlers 

    public  void checkWhiteCheck(int arayPos){
        updateBoard();
        whiteIsInCheck = false;
        long BB = (Board.blackBishops )& MagicBitBoards.getBishopAttacks(arayPos, Board.blackBoard);
        if (BB != 0L){
            int point = Long.numberOfTrailingZeros(BB);
            long CC = (MagicBitBoards.getBishopAttacks(arayPos, Board.blackBoard) & MagicBitBoards.getBishopAttacks(point, Board.whiteBoard))|BB;
            if (CC ==(CC & ~ Board.whiteBoard)){
                Board.kingCheckSquares = CC; //(Board.blackBishops | Board.blackQueen)& MagicBitBoards.getBishopAttacks(arayPos, Board.board)
                whiteIsInCheck = true;
            }
            Board.checkSquares = CC ;
        }

        long RR = (Board.blackRoocks ) & MagicBitBoards.getRookAttacks(arayPos, Board.blackBoard) ;
        if (RR != 0L){
            
            int point = Long.numberOfTrailingZeros(RR);
            long CC  = (MagicBitBoards.getRookAttacks(arayPos, Board.blackBoard) & MagicBitBoards.getRookAttacks(point, Board.whiteBoard))|RR;
            if (CC == (CC & ~ Board.whiteBoard)){
                Board.kingCheckSquares = CC ;//(Board.blackRoocks | Board.blackQueen) & MagicBitBoards.getRookAttacks(arayPos, Board.board)
                whiteIsInCheck = true;
            }
            Board.checkSquares = CC;
        }

        long QB = ( Board.blackQueen)& MagicBitBoards.getBishopAttacks(arayPos, Board.blackBoard);
        if (QB != 0L){
            int point = Long.numberOfTrailingZeros(QB);
            long CC = (MagicBitBoards.getBishopAttacks(arayPos, Board.blackBoard) & MagicBitBoards.getBishopAttacks(point, Board.whiteBoard))|QB;
            if (CC ==(CC & ~ Board.whiteBoard)){
                Board.kingCheckSquares = CC;//(Board.blackBishops | Board.blackQueen)& MagicBitBoards.getBishopAttacks(arayPos, Board.board)
                whiteIsInCheck = true;
            }
            Board.checkSquares = CC ;
        }

        long QR = ( Board.blackQueen) & MagicBitBoards.getRookAttacks(arayPos, Board.blackBoard) ;
        if (QR != 0L){
            
            int point = Long.numberOfTrailingZeros(QR);
            long CC  = (MagicBitBoards.getRookAttacks(arayPos, Board.blackBoard) & MagicBitBoards.getRookAttacks(point, Board.whiteBoard))|QR;
            if (CC == (CC & ~ Board.whiteBoard)){
                Board.kingCheckSquares = CC ;//(Board.blackRoocks | Board.blackQueen) & MagicBitBoards.getRookAttacks(arayPos, Board.board)
                whiteIsInCheck = true;
            }
            Board.checkSquares = CC;
        }

        long KK = Board.blackKnights & MagicBitBoards.KNIGHT_ATTACKS[arayPos];
        if (KK != 0L){
            Board.kingCheckSquares =KK;
            whiteIsInCheck = true;
        }

        long kk = Board.blackKing & MagicBitBoards.KING_ATTACKS[arayPos];
        if (kk != 0L){
            Board.kingCheckSquares =kk;
            whiteIsInCheck = true;
        }

        long pp = Board.blackPawns & MagicBitBoards.PAWN_ATTACKS[0][arayPos];
        if (pp != 0L){
            Board.kingCheckSquares = pp;
            whiteIsInCheck = true;
        }
    }

    public  void checkBlackCheck(int arayPos){
        blackIsInCheck = false;

        long BB = (Board.whiteBishops )& MagicBitBoards.getBishopAttacks(arayPos, Board.whiteBoard);
        if (BB != 0L){
            int point = Long.numberOfTrailingZeros(BB);
            long CC =(MagicBitBoards.getBishopAttacks(arayPos, Board.whiteBoard) & MagicBitBoards.getBishopAttacks(point, Board.blackBoard))|BB;
            if (CC == (CC & ~ Board.blackBoard) ){
                blackIsInCheck = true;
                Board.kingCheckSquares =CC;
            }
            Board.checkSquares = CC;
            
        }

        long RR = (Board.whiteRoocks)& MagicBitBoards.getRookAttacks(arayPos, Board.whiteBoard);
        if (RR != 0L){
            int point = Long.numberOfTrailingZeros(RR);
            long CC = (MagicBitBoards.getRookAttacks(arayPos, Board.whiteBoard) & MagicBitBoards.getRookAttacks(point, Board.blackBoard))|RR;
            if (CC == (CC & ~ Board.blackBoard) ){
                blackIsInCheck = true;
                Board.kingCheckSquares =CC;
            }
            Board.checkSquares = CC;
        }

        long QB = ( Board.whiteQueen)& MagicBitBoards.getBishopAttacks(arayPos, Board.whiteBoard);
        if (QB != 0L){
            int point = Long.numberOfTrailingZeros(QB);
            long CC =(MagicBitBoards.getBishopAttacks(arayPos, Board.whiteBoard) & MagicBitBoards.getBishopAttacks(point, Board.blackBoard))|QB;
            if (CC == (CC & ~ Board.blackBoard) ){
                blackIsInCheck = true;
                Board.kingCheckSquares =CC;
            }
            Board.checkSquares = CC;
            
        }

        long QR = (Board.whiteRoocks | Board.whiteQueen)& MagicBitBoards.getRookAttacks(arayPos, Board.whiteBoard);
        if (QR != 0L){
            int point = Long.numberOfTrailingZeros(QR);
            long CC = (MagicBitBoards.getRookAttacks(arayPos, Board.whiteBoard) & MagicBitBoards.getRookAttacks(point, Board.blackBoard))|QR;
            if (CC == (CC & ~ Board.blackBoard) ){
                blackIsInCheck = true;
                Board.kingCheckSquares = CC;
            }
            Board.checkSquares = CC;
        }

        long KK = Board.whiteKnights & MagicBitBoards.KNIGHT_ATTACKS[arayPos];
        if (KK != 0L){
            Board.kingCheckSquares = KK ;
            blackIsInCheck = true;
        }

        long kk = Board.whiteKing & MagicBitBoards.KING_ATTACKS[arayPos];
        if (kk != 0L){
            Board.kingCheckSquares =kk;
            blackIsInCheck = true;
        }

        long pp = Board.whitePawns & MagicBitBoards.PAWN_ATTACKS[1][arayPos];
        if (pp != 0L){
            Board.kingCheckSquares = pp;
            blackIsInCheck = true;
        }
    }

    public  boolean checkWhiteMateCheck(){
        updateBlackBoard();

        for (int i = 0 ; i<64 ; i++){
            Board.atackSquares =0;
            if (((Board.whiteBoard >>> i)& 1L) ==1){
                int piece = returnPiece(i);
                if (piece == 1){
                    WPMouvement(i);
                }if (piece == 2){
                    WBMouvement(i);
                }if (piece == 3){
                    WRMouvement(i);
                }if (piece == 4){
                    WQMouvement(i);
                }if (piece == 5){
                    WKNMovement(i);
                }if (piece == 6){
                    WKMovement(i);
                }
                
                if (Board.atackSquares != 0) {
                    return false;

                }
            }
        }
        return true;
    }

    public  boolean checkBlackMateCheck(){
        updateWhiteBoard();
        
        for (int i =0 ;i<64;i++){
            if (((Board.blackBoard >>> i) & 1L) != 0){

                
                Board.atackSquares = 0;
                Board.captureSquares = 0;
                int pieceType = returnPiece(i);
                switch (pieceType) {
                    case 7 -> BPMouvement(i);
                    case 8 -> BBMouvement(i);
                    case 9 -> BRMouvement(i);
                    case 10 -> BQMouvement(i);
                    case 11 -> BKNMovement(i);
                    case 12 -> BKMovement(i);

                }

                if (Board.atackSquares !=0){
                    return false;
                }

            }
        }
        return true;
    }

    public  void WhiteAtackSquares(){
        Board.whiteAtackSquares = 0L;
        long B = Board.board & ~ Board.blackKing;
        for (int i = 0 ; i<64 ; i++){
            int piece ;
            if ((((Board.whiteBoard )>>> i)& 1L) ==1){
                piece = returnPiece(i);

                if (piece == 1){
                    Board.whiteAtackSquares |=  MagicBitBoards.PAWN_ATTACKS[0][i];
                }if (piece == 2){
                    Board.whiteAtackSquares |= MagicBitBoards.getBishopAttacks(i, B);
                }if (piece == 3){
                    Board.whiteAtackSquares |= MagicBitBoards.getRookAttacks(i, B);
                }if (piece == 4){
                    Board.whiteAtackSquares |= MagicBitBoards.getQueenAttacks(i, B);
                }if (piece == 5){
                    Board.whiteAtackSquares |= MagicBitBoards.KNIGHT_ATTACKS[i];
                }if (piece == 6){
                    Board.whiteAtackSquares |= MagicBitBoards.KING_ATTACKS[i];
                }
            }
        }
    }

    public  void BlackAtackSquares(){
        Board.BlackAtackSquares =0L;
        long B = Board.board & ~ Board.whiteKing;
        for (int i = 0 ; i<64 ; i++){
            int piece ;
            if (((Board.blackBoard >>> i)& 1L) ==1){
                piece = returnPiece(i);

                if (piece == 7){
                    Board.BlackAtackSquares |=  MagicBitBoards.PAWN_ATTACKS[1][i];
                }if (piece == 8){
                    Board.BlackAtackSquares |= MagicBitBoards.getBishopAttacks(i, B);
                }if (piece == 9){
                    Board.BlackAtackSquares |= MagicBitBoards.getRookAttacks(i, B);
                }if (piece == 10){
                    Board.BlackAtackSquares |= MagicBitBoards.getQueenAttacks(i, B);
                }if (piece == 11){
                    Board.BlackAtackSquares |= MagicBitBoards.KNIGHT_ATTACKS[i];
                }if (piece == 12){
                    Board.BlackAtackSquares |= MagicBitBoards.KING_ATTACKS[i];
                }
            }
        }
    }

    // board updaters

    public  void updateWhiteBoard(){
        Board.whiteBoard = 0;
        Board.whiteBoard = Board.whiteBishops | Board.whiteKing | Board.whiteKnights | Board.whitePawns | Board.whiteQueen | Board.whiteRoocks;
        
    }

    public  void updateBlackBoard(){
        Board.blackBoard = 0;
        Board.blackBoard = Board.blackBishops | Board.blackKing | Board.blackKnights | Board.blackPawns | Board.blackQueen | Board.blackRoocks;
    }

    public  void updateBoard(){
        Board.board = 0L;
        Board.board = Board.blackBishops | Board.blackKing | Board.blackKnights | Board.blackPawns | Board.blackQueen | Board.blackRoocks | 
                      Board.whiteBishops | Board.whiteKing | Board.whiteKnights | Board.whitePawns | Board.whiteQueen | Board.whiteRoocks;
    }

    // human promotion helper + Castling

    /** Human chose a piece for a black pawn waiting on the 1st rank. 1=knight 2=rook 3=queen 4=bishop. */
    public  void blackPromotion(int arayPos) {
        if (!blackPromotionUI) {
            return;
        }
        long bit = 1L << arayPos;
        Board.blackPawns &= ~bit;
        switch (promotionPiece) {
            case 1 -> Board.blackKnights |= bit;
            case 2 -> Board.blackRoocks |= bit;
            case 4 -> Board.blackBishops |= bit;
            default -> Board.blackQueen |= bit;   // queen is the safe default
        }
        blackPromotionUI = false;
        promotionPiece = 0;
        finishMove(arayPos);
    }

    /** Human chose a piece for a white pawn waiting on the 8th rank. 1=knight 2=rook 3=queen 4=bishop. */
    public  void whitePromotion(int arayPos) {
        if (!whitePromotionUI) {
            return;
        }
        long bit = 1L << arayPos;
        Board.whitePawns &= ~bit;
        switch (promotionPiece) {
            case 1 -> Board.whiteKnights |= bit;
            case 2 -> Board.whiteRoocks |= bit;
            case 4 -> Board.whiteBishops |= bit;
            default -> Board.whiteQueen |= bit;   // queen is the safe default
        }
        whitePromotionUI = false;
        promotionPiece = 0;
        finishMove(arayPos);
    }


    /*
     * Castling rules (bit 0 = h1, bit 3 = e1, bit 7 = a1, bit 56 = h8, bit 59 = e8, bit 63 = a8):
     *  - king and that rook have never moved (a captured rook also loses the right),
     *  - every square between them is empty,
     *  - the king is NOT in check now, and does not pass through or land on an attacked square.
     *  Having been in check earlier in the game does NOT matter.
     * Must be called right after WKMovement/BKMovement computed the enemy attack map.
     */
    public  void checkWhiteCastling(){
        if (WhiteKingHasBeenMoved || Board.whiteKing != (1L << 3)) return;
        long enemy = Board.BlackAtackSquares;
        if ((enemy & (1L << 3)) != 0) return;                              // in check: no castling

        // O-O: f1(2), g1(1) empty and not attacked, rook on h1(0)
        long kingSidePath = (1L << 1) | (1L << 2);
        if (!WhiteRightRookHasBeenMoved && (Board.whiteRoocks & 1L) != 0
                && (Board.board & kingSidePath) == 0 && (enemy & kingSidePath) == 0) {
            Board.atackSquares |= (1L << 1);
        }

        // O-O-O: d1(4), c1(5), b1(6) empty; d1 and c1 not attacked (b1 may be); rook on a1(7)
        long queenSideEmpty = (1L << 4) | (1L << 5) | (1L << 6);
        long queenSideSafe = (1L << 4) | (1L << 5);
        if (!WhiteLeftRookHasBeenMoved && (Board.whiteRoocks & (1L << 7)) != 0
                && (Board.board & queenSideEmpty) == 0 && (enemy & queenSideSafe) == 0) {
            Board.atackSquares |= (1L << 5);
        }
    }

    public  void checkBlackCastling(){
        if (blackKingHasBeenMoved || Board.blackKing != (1L << 59)) return;
        long enemy = Board.whiteAtackSquares;
        if ((enemy & (1L << 59)) != 0) return;                             // in check: no castling

        // O-O: f8(58), g8(57) empty and not attacked, rook on h8(56)
        long kingSidePath = (1L << 57) | (1L << 58);
        if (!BlackRightRookHasBeenMoved && (Board.blackRoocks & (1L << 56)) != 0
                && (Board.board & kingSidePath) == 0 && (enemy & kingSidePath) == 0) {
            Board.atackSquares |= (1L << 57);
        }

        // O-O-O: d8(60), c8(61), b8(62) empty; d8 and c8 not attacked; rook on a8(63)
        long queenSideEmpty = (1L << 60) | (1L << 61) | (1L << 62);
        long queenSideSafe = (1L << 60) | (1L << 61);
        if (!BlackLeftRookHasBeenMoved && (Board.blackRoocks & (1L << 63)) != 0
                && (Board.board & queenSideEmpty) == 0 && (enemy & queenSideSafe) == 0) {
            Board.atackSquares |= (1L << 61);
        }
    }

    // state handlers

    public record State(
            int pieceMoved,
            int pieceMovedPos,
            int SpieceMoved,
            int SpieceMovedPos,
            boolean isCapture,
            int pieceCapuredSquare,
            int pieceCapured,
            boolean WhiteKingHasBeenMoved,
            boolean blackKingHasBeenMoved,
            boolean WhiteKingHasBeenChecked,
            boolean blackKingHasBeenChecked,
            int whiteKingPos,
            int blackKingPos,
            boolean whiteIsInCheck,
            boolean blackIsInCheck,
            boolean turn,
            boolean gameFinished,
            boolean whitePromotionUI,
            boolean blackPromotionUI,
            int promotionPiece,
            int promotionsquare,
            boolean WhiteLeftRookHasBeenMoved,
            boolean WhiteRightRookHasBeenMoved,
            boolean BlackLeftRookHasBeenMoved,
            boolean BlackRightRookHasBeenMoved,
            boolean BlackAi,
            int won,
            int count
    ) {
    }

    private  State savedState;

    public  State captureState() {
        return new State(
                pieceMoved,
                pieceMovedPos,
                SpieceMoved,
                SpieceMovedPos,
                isCapture,
                pieceCapuredSquare,
                pieceCapured,
                WhiteKingHasBeenMoved,
                blackKingHasBeenMoved,
                WhiteKingHasBeenChecked,
                blackKingHasBeenChecked,
                whiteKingPos,
                blackKingPos,
                whiteIsInCheck,
                blackIsInCheck,
                turn,
                gameFinished,
                whitePromotionUI,
                blackPromotionUI,
                promotionPiece,
                promotionsquare,
                WhiteLeftRookHasBeenMoved,
                WhiteRightRookHasBeenMoved,
                BlackLeftRookHasBeenMoved,
                BlackRightRookHasBeenMoved,
                BlackAi,
                won,
                count
        );
    }

    public  void saveState() {
        savedState = captureState();
    }

    public  void resetState(State state) {
        if (state == null) {
            return;
        }

        pieceMoved = state.pieceMoved();
        pieceMovedPos = state.pieceMovedPos();
        SpieceMoved = state.SpieceMoved();
        SpieceMovedPos = state.SpieceMovedPos();
        isCapture = state.isCapture();
        pieceCapuredSquare = state.pieceCapuredSquare();
        pieceCapured = state.pieceCapured();

        WhiteKingHasBeenMoved = state.WhiteKingHasBeenMoved();
        blackKingHasBeenMoved = state.blackKingHasBeenMoved();

        WhiteKingHasBeenChecked = state.WhiteKingHasBeenChecked();
        blackKingHasBeenChecked = state.blackKingHasBeenChecked();

        whiteKingPos = state.whiteKingPos();
        blackKingPos = state.blackKingPos();

        whiteIsInCheck = state.whiteIsInCheck();
        blackIsInCheck = state.blackIsInCheck();

        turn = state.turn();
        gameFinished = state.gameFinished();

        whitePromotionUI = state.whitePromotionUI();
        blackPromotionUI = state.blackPromotionUI();

        promotionPiece = state.promotionPiece();
        promotionsquare = state.promotionsquare();

        WhiteLeftRookHasBeenMoved = state.WhiteLeftRookHasBeenMoved();
        WhiteRightRookHasBeenMoved = state.WhiteRightRookHasBeenMoved();
        BlackLeftRookHasBeenMoved = state.BlackLeftRookHasBeenMoved();
        BlackRightRookHasBeenMoved = state.BlackRightRookHasBeenMoved();

        BlackAi = state.BlackAi();
        won = state.won();
        count = state.count();
    }

    public boolean randomStartingColor = false;

    public void resetGame() {
        Board.resetToStandard();

        // Only meaningful in human-vs-AI games: randomly choose which side the human plays.
        // Human-vs-human and AI-vs-AI keep the players exactly as chosen.
        if (randomStartingColor && whiteAi != BlackAi) {
            boolean playerIsWhite = Math.random() < 0.5;
            whiteAi = !playerIsWhite;
            BlackAi = playerIsWhite;
        }

        turn = true;
        gameFinished = false;
        whitePromotionUI = false;
        blackPromotionUI = false;
        promotionPiece = 0;
        promotionsquare = 0;

        int wKingIdx = Long.numberOfTrailingZeros(Board.whiteKing);
        whiteKingPos = (wKingIdx >= 0 && wKingIdx < 64) ? wKingIdx : 3;

        int bKingIdx = Long.numberOfTrailingZeros(Board.blackKing);
        blackKingPos = (bKingIdx >= 0 && bKingIdx < 64) ? bKingIdx : 59;

        whiteIsInCheck = false;
        blackIsInCheck = false;
        WhiteKingHasBeenMoved = false;
        blackKingHasBeenMoved = false;
        WhiteLeftRookHasBeenMoved = false;
        WhiteRightRookHasBeenMoved = false;
        BlackLeftRookHasBeenMoved = false;
        BlackRightRookHasBeenMoved = false;
        won = 0;
        count = 0;

        Board.atackSquares = 0L;
        Board.captureSquares = 0L;
        Board.move = 0L;
        Board.checkSquares = 0L;
        Board.kingCheckSquares = 0L;

        updateWhiteBoard();
        updateBlackBoard();
        updateBoard();
        saveState();
    }

    public  void resetState() {
        resetGame();
    }

}