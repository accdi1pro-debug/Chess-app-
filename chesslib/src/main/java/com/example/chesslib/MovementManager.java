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
                    if (!WhiteKingHasBeenChecked && !WhiteKingHasBeenMoved){
                        if (arayPos == 1&& !WhiteRightRookHasBeenMoved){
                            Board.whiteRoocks ^= (1L) ;

                            Board.whiteRoocks ^= (1L << 2) ;
                        }
                        if (arayPos == 5 && !WhiteLeftRookHasBeenMoved){
                            Board.whiteRoocks ^= (1L << 7) ;

                            Board.whiteRoocks ^= (1L << 4) ;
                        }
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
                    if (!blackKingHasBeenChecked && !blackKingHasBeenMoved){
                        if (arayPos == 61 && !BlackLeftRookHasBeenMoved){
                            Board.blackRoocks ^= (1L << 63) ;

                            Board.blackRoocks ^= (1L << 60) ;
                        }
                        if (arayPos == 57 && !BlackRightRookHasBeenMoved){
                            Board.blackRoocks ^= (1L << 56) ;

                            Board.blackRoocks ^= (1L << 58) ;
                        }
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

    public  void blackPromotion(int arayPos) {
        Board.blackPawns ^= (1L << arayPos);

        switch (promotionPiece) {
            case 1 -> Board.blackKnights ^= (1L << arayPos);
            case 2 -> Board.blackRoocks ^= (1L << arayPos);
            case 3 -> Board.blackQueen ^= (1L << arayPos);
            case 4 -> Board.blackBishops ^= (1L << arayPos);
            default -> Board.blackBishops ^= (1L << arayPos);
        }
        blackPromotionUI = false;

        promotionPiece = 0;

        Board.atackSquares = 0;
        Board.captureSquares = 0;

        Board.checkSquares = 0;
        updateWhiteBoard();
        updateBlackBoard();

        if (turn == true){
            checkBlackCheck(blackKingPos);
            
            if (blackIsInCheck){

                blackKingHasBeenChecked = true;
            }
            WhiteAtackSquares();
            
        }else{
            checkWhiteCheck(whiteKingPos);
            if (whiteIsInCheck){
                WhiteKingHasBeenChecked = true;
            }
            BlackAtackSquares();
            
        }


        if (count >= 50){
            gameFinished = true;
        }

        if (whiteIsInCheck && checkWhiteMateCheck()){
            gameFinished = true;
        }
        if (blackIsInCheck && checkBlackMateCheck()){
            gameFinished = true;
        }

        turn = !turn ;

        
        
        Board.atackSquares = 0;
        Board.captureSquares = 0;
        pieceMoved = 0;
        pieceMovedPos = 0;

        
    }

    public  void whitePromotion(int arayPos) {
        Board.whitePawns ^= (1L << arayPos);

        switch (promotionPiece) {
            case 1 -> Board.whiteKnights ^= (1L << arayPos);
            case 2 -> Board.whiteRoocks ^= (1L << arayPos);
            case 3 -> Board.whiteQueen ^= (1L << arayPos);
            case 4 -> Board.whiteBishops ^= (1L << arayPos);
            default -> Board.whiteBishops ^= (1L << arayPos);
        }

        promotionPiece = 0;
        whitePromotionUI = false;

        Board.atackSquares = 0;
        Board.captureSquares = 0;

        Board.checkSquares = 0;
        updateWhiteBoard();
        updateBlackBoard();

        if (turn == true){
            checkBlackCheck(blackKingPos);
            
            if (blackIsInCheck){
                blackKingHasBeenChecked = true;
            }
            WhiteAtackSquares();
            
        }else{
            checkWhiteCheck(whiteKingPos);
            if (whiteIsInCheck){
                WhiteKingHasBeenChecked = true;
            }
            BlackAtackSquares();
            
        }


        if (count >= 50){
            gameFinished = true;
        }

        if (whiteIsInCheck && checkWhiteMateCheck()){
            gameFinished = true;
        }
        if (blackIsInCheck && checkBlackMateCheck()){
            gameFinished = true;
        }

        turn = !turn ;

        
        
        Board.atackSquares = 0;
        Board.captureSquares = 0;
        pieceMoved = 0;
        pieceMovedPos = 0;

        
    }


    public  void checkWhiteCastling(){
        long right = 6L;
        if ((Board.whiteRoocks & 1L ) != 0 && !checkIfSquareisUsed(1) && !checkIfSquareisUsed(2) && !WhiteRightRookHasBeenMoved && (right & Board.allAtackSquares)==0L) {
            Board.atackSquares |= (1L << 1);
        }
        long left = 112L;
        if (((Board.whiteRoocks >>> 7) & 1L) != 0 && !checkIfSquareisUsed(6) && !checkIfSquareisUsed(5) && !checkIfSquareisUsed(4) && !WhiteLeftRookHasBeenMoved && (left & Board.BlackAtackSquares)==0L) {
            Board.atackSquares |= (1L << 5);
        }
    }
     
    public  void checkBlackCastling(){      
        long left = (1L << 58) | (1L << 57);
        if (((Board.blackRoocks >>> 56) & 1L) != 0 && !checkIfSquareisUsed(57) && !checkIfSquareisUsed(58)&& !BlackLeftRookHasBeenMoved && (left & Board.allAtackSquares)==0L ) {
            Board.atackSquares |= (1L << 57);
        }
        long right = (1L << 62) | (1L << 61) | (1L << 60);
        if (((Board.blackRoocks >>> 63) & 1L) != 0 && !checkIfSquareisUsed(62) && !checkIfSquareisUsed(61) && !checkIfSquareisUsed(60) && !BlackRightRookHasBeenMoved && (right & Board.whiteAtackSquares)==0L) {
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

        if (randomStartingColor) {
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