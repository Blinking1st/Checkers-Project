package Checkers;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Text-based version of the Checkers game. Players interact entirely through
 * the console. An ASCII board is printed after every move.
 *
 * Piece symbols:
 *   r = Red normal     b = Black normal
 *   R = Red King       B = Black King
 *   . = empty dark tile
 *   # = unavailable light tile
 */
public class TextGame {

    private int ROW, COL, pieceRows, numPieces;
    private Tiles[][] theBoard;
    private ArrayList<Pieces> redPieces  = new ArrayList<>();
    private ArrayList<Pieces> blackPieces = new ArrayList<>();
    private int redCounter = 0, blackCounter = 0;
    private Player RED, BLACK;
    private int turnCounter = 0;
    private int destRow = 0, destCol = 0;
    private int currentRow = 0, currentCol = 0;
    private int preyRow = 0, preyCol = 0;
    private Pieces lastPieceMoved;
    private boolean gameOver = false;
    private final Scanner scanner = new Scanner(System.in);

    public TextGame() {
        chooseBoardSize();
        initBoard();
        makePieces();
        RED   = new Player(PlayerType.RED,   numPieces);
        BLACK = new Player(PlayerType.BLACK, numPieces);
        gameLoop();
        scanner.close();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private void chooseBoardSize() {
        System.out.println("Select board size:");
        System.out.println("  1 - 8x8  (12 pieces each)");
        System.out.println("  2 - 10x10 (20 pieces each)");
        System.out.print("Enter choice (1 or 2): ");
        String input = scanner.nextLine().trim();
        if (input.equals("2")) {
            ROW = 10; COL = 10; pieceRows = 4; numPieces = 20;
            System.out.println("10x10 board selected.");
        } else {
            ROW = 8; COL = 8; pieceRows = 3; numPieces = 12;
            System.out.println("8x8 board selected.");
        }
    }

    private void initBoard() {
        // Tiles are reused for piece storage. null board ref is safe in text
        // mode because Tiles.selected() (which calls board.movePieces) is
        // never triggered — there is no mouse listener.
        theBoard = new Tiles[ROW][COL];
        for (int i = 0; i < ROW; i++)
            for (int j = 0; j < COL; j++)
                theBoard[i][j] = new Tiles(i, j, null);
    }

    private void makePieces() {
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if ((i + j) % 2 == 1) {
                    if (i < pieceRows) {
                        redPieces.add(new Pieces(i, j, PieceType.RED));
                        theBoard[i][j].addPiece(redPieces.get(redCounter++));
                    } else if (i >= ROW - pieceRows) {
                        blackPieces.add(new Pieces(i, j, PieceType.BLACK));
                        theBoard[i][j].addPiece(blackPieces.get(blackCounter++));
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ASCII board rendering
    // -------------------------------------------------------------------------

    private void printBoard() {
        System.out.println();
        // Column header
        System.out.print("     ");
        for (int j = 0; j < COL; j++) System.out.printf(" %-2d ", j);
        System.out.println();
        printDivider();
        for (int i = 0; i < ROW; i++) {
            System.out.printf(" %2d |", i);
            for (int j = 0; j < COL; j++) {
                if ((i + j) % 2 == 0) {
                    System.out.print("###|");
                } else if (!theBoard[i][j].isOccupied()) {
                    System.out.print(" . |");
                } else {
                    switch (theBoard[i][j].getPiece().getType()) {
                        case RED:        System.out.print(" r |"); break;
                        case BLACK:      System.out.print(" b |"); break;
                        case RED_KING:   System.out.print(" R |"); break;
                        case BLACK_KING: System.out.print(" B |"); break;
                    }
                }
            }
            System.out.println();
            printDivider();
        }
        System.out.println("  r=Red  b=Black  R=Red King  B=Black King  .=empty  #=unavailable");
        System.out.println();
    }

    private void printDivider() {
        System.out.print("    +");
        for (int j = 0; j < COL; j++) System.out.print("---+");
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Game state helpers
    // -------------------------------------------------------------------------

    private PlayerType turn() {
        return turnCounter % 2 == 1 ? PlayerType.RED : PlayerType.BLACK;
    }

    private void switchTurns() {
        turnCounter++;
    }

    private void checkWin() {
        if (RED.piecesLeft() == 0) {
            printBoard();
            System.out.println("Game over! Red-side player lost! Well played!");
            gameOver = true;
        } else if (BLACK.piecesLeft() == 0) {
            printBoard();
            System.out.println("Game over! Black-side player lost! Well played!");
            gameOver = true;
        }
    }

    private void checkingTheCrown(Pieces p, int dRow, int dCol) {
        if (p.getType() == PieceType.RED && dRow == ROW - 1) {
            p.crowned();
            System.out.println("  >> Red piece crowned as King!");
        } else if (p.getType() == PieceType.BLACK && dRow == 0) {
            p.crowned();
            System.out.println("  >> Black piece crowned as King!");
        }
    }

    // -------------------------------------------------------------------------
    // Jump logic (mirrors Board.java)
    // -------------------------------------------------------------------------

    private boolean checkJump(Pieces jumper) {
        currentRow = jumper.getRow();
        currentCol = jumper.getCol();
        int rowDistance = destRow - jumper.getRow();
        int colDistance = destCol - jumper.getCol();
        preyRow = jumper.getRow() + (rowDistance / 2);
        preyCol = jumper.getCol() + (colDistance / 2);
        if (preyRow >= 0 && preyRow <= ROW - 1 && preyCol >= 0 && preyCol <= COL - 1) {
            return !theBoard[destRow][destCol].isOccupied()
                    && theBoard[preyRow][preyCol].isOccupied();
        }
        return false;
    }

    private boolean jumpAvailable(Pieces jumper) {
        int switchCase = 0, rowMove = 0;
        if (jumper.getType() == PieceType.RED) {
            switchCase = 1; rowMove = 2;
        } else if (jumper.getType() == PieceType.BLACK) {
            switchCase = 2; rowMove = -2;
        } else if (jumper.getType() == PieceType.RED_KING
                || jumper.getType() == PieceType.BLACK_KING) {
            switchCase = 3;
        }
        int jr = jumper.getRow(), jc = jumper.getCol();

        switch (switchCase) {
            case 1: {
                if (jr > -1 && jr < ROW && jc > -1 && jc < COL) {
                    if ((jr + rowMove) <= ROW - 1) {
                        if (jc != COL - 1 && jc < COL - 2 && jc != 0 && jc > 1) {
                            if (!theBoard[jr + rowMove][jc + 2].isOccupied() && theBoard[jr + 1][jc + 1].isOccupied()) return true;
                            if (!theBoard[jr + rowMove][jc - 2].isOccupied() && theBoard[jr + 1][jc - 1].isOccupied()) return true;
                            return false;
                        }
                        if (jc >= COL - 2)
                            if (!theBoard[jr + rowMove][jc - 2].isOccupied() && theBoard[jr + 1][jc - 1].isOccupied()) return true;
                        if (jc <= 1)
                            if (!theBoard[jr + rowMove][jc + 2].isOccupied() && theBoard[jr + 1][jc + 1].isOccupied()) return true;
                        return false;
                    }
                }
            } break;
            case 2: {
                if (jr > -1 && jr < ROW && jc > -1 && jc < COL) {
                    if ((jr + rowMove) > -1) {
                        if (jc != COL - 1 && jc < COL - 2 && jc != 0 && jc > 1) {
                            if (!theBoard[jr + rowMove][jc + 2].isOccupied() && theBoard[jr - 1][jc + 1].isOccupied()) return true;
                            if (!theBoard[jr + rowMove][jc - 2].isOccupied() && theBoard[jr - 1][jc - 1].isOccupied()) return true;
                            return false;
                        }
                        if (jc >= COL - 2)
                            if (!theBoard[jr + rowMove][jc - 2].isOccupied() && theBoard[jr - 1][jc - 1].isOccupied()) return true;
                        if (jc <= 1)
                            if (!theBoard[jr + rowMove][jc + 2].isOccupied() && theBoard[jr - 1][jc + 1].isOccupied()) return true;
                        return false;
                    }
                }
            } break;
            case 3: {
                int kn = jr - 2, ke = jc + 2, ks = jr + 2, kw = jc - 2;
                if (ks <= ROW - 1 && ke <= COL - 1 && kn >= 0 && kw >= 0) {
                    if (!theBoard[kn][ke].isOccupied() && theBoard[jr - 1][jc + 1].isOccupied()) return true;
                    if (!theBoard[kn][kw].isOccupied() && theBoard[jr - 1][jc - 1].isOccupied()) return true;
                    if (!theBoard[ks][ke].isOccupied() && theBoard[jr + 1][jc + 1].isOccupied()) return true;
                    if (!theBoard[ks][kw].isOccupied() && theBoard[jr + 1][jc - 1].isOccupied()) return true;
                }
                if ((jr == 0 || jr == 1) && ke <= COL - 1 && kw >= 0) {
                    if (!theBoard[ks][ke].isOccupied() && theBoard[jr + 1][jc + 1].isOccupied()) return true;
                    if (!theBoard[ks][kw].isOccupied() && theBoard[jr + 1][jc - 1].isOccupied()) return true;
                }
                if ((jr == ROW - 1 || jr == ROW - 2) && ke <= COL - 3 && kw >= 0) {
                    if (!theBoard[kn][ke].isOccupied() && theBoard[jr - 1][jc + 1].isOccupied()) return true;
                    if (!theBoard[kn][kw].isOccupied() && theBoard[jr - 1][jc - 1].isOccupied()) return true;
                }
                if ((jr == ROW - 1 || jr == ROW - 2) && kn >= 0 && ks <= ROW - 3) {
                    if (!theBoard[kn][kw].isOccupied() && theBoard[jr - 1][jc - 1].isOccupied()) return true;
                    if (!theBoard[ks][kw].isOccupied() && theBoard[jr + 1][jc - 1].isOccupied()) return true;
                }
                if ((jr == 0 || jr == 1) && kn >= 0 && ks <= ROW - 3) {
                    if (!theBoard[kn][ke].isOccupied() && theBoard[jr - 1][jc + 1].isOccupied()) return true;
                    if (!theBoard[ks][ke].isOccupied() && theBoard[jr + 1][jc + 1].isOccupied()) return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Executes a jump from currentRow/currentCol to destRow/destCol.
     * preyRow/preyCol must already be set by checkJump.
     * @return true if the jump was valid and executed
     */
    private boolean executeJump(Pieces jumper) {
        Pieces prey = theBoard[preyRow][preyCol].getPiece();
        if (prey == null) {
            System.out.println("No piece to jump over.");
            return false;
        }
        if (jumper.getType() == prey.getType()
                || (isRed(jumper) && isRed(prey))
                || (isBlack(jumper) && isBlack(prey))) {
            System.out.println("Cannot jump over your own piece.");
            return false;
        }
        theBoard[destRow][destCol].addPiece(jumper);
        theBoard[currentRow][currentCol].delete();
        jumper.moved(destRow, destCol);
        lastPieceMoved = jumper;
        checkingTheCrown(jumper, destRow, destCol);
        theBoard[preyRow][preyCol].delete();
        if (isRed(prey))   RED.pieceEaten();
        else               BLACK.pieceEaten();
        checkWin();
        return true;
    }

    private boolean isRed(Pieces p) {
        return p.getType() == PieceType.RED || p.getType() == PieceType.RED_KING;
    }

    private boolean isBlack(Pieces p) {
        return p.getType() == PieceType.BLACK || p.getType() == PieceType.BLACK_KING;
    }

    // -------------------------------------------------------------------------
    // Main game loop
    // -------------------------------------------------------------------------

    private void gameLoop() {
        System.out.println("\nWelcome to Checkers! Black moves first.");
        System.out.println("Enter coordinates as: row col  (e.g. '2 3')");
        System.out.println("Type 'resign' at any prompt to concede.\n");
        printBoard();

        while (!gameOver) {
            PlayerType currentTurn = turn();
            System.out.println("=== " + currentTurn + "'s turn ===");

            // --- select piece ---
            int[] from = promptCoords("Select piece   (row col): ");
            if (from == null) continue;
            if (from[0] == -1) { resign(currentTurn); break; }

            int fromRow = from[0], fromCol = from[1];
            if (!inBounds(fromRow, fromCol)) { System.out.println("Out of bounds."); continue; }
            if (!theBoard[fromRow][fromCol].isOccupied()) { System.out.println("No piece there."); continue; }
            Pieces selected = theBoard[fromRow][fromCol].getPiece();
            if (selected.getSide() != currentTurn) { System.out.println("That is not your piece."); continue; }

            // --- select destination ---
            int[] to = promptCoords("Select destination (row col): ");
            if (to == null) continue;
            if (to[0] == -1) { resign(currentTurn); break; }

            int toRow = to[0], toCol = to[1];
            if (!inBounds(toRow, toCol)) { System.out.println("Out of bounds."); continue; }
            if ((toRow + toCol) % 2 == 0) { System.out.println("Cannot move to a white tile."); continue; }

            // --- execute move ---
            currentRow = fromRow; currentCol = fromCol;
            destRow    = toRow;   destCol    = toCol;

            boolean moveMade = false;

            if (jumpAvailable(selected)) {
                if (!checkJump(selected)) {
                    System.out.println("A jump is available — that destination is not a valid jump. Try again.");
                    continue;
                }
                if (!executeJump(selected)) continue;
                moveMade = true;

                // chain jumps: same piece must keep jumping while able
                while (!gameOver && lastPieceMoved != null && jumpAvailable(lastPieceMoved)) {
                    printBoard();
                    System.out.println("Chain jump available — same piece must jump again.");
                    int[] chainTo = promptCoords("Select jump destination (row col): ");
                    if (chainTo == null || chainTo[0] == -1) break;
                    currentRow = lastPieceMoved.getRow();
                    currentCol = lastPieceMoved.getCol();
                    destRow = chainTo[0]; destCol = chainTo[1];
                    if (!checkJump(lastPieceMoved)) { System.out.println("Invalid jump."); break; }
                    if (!executeJump(lastPieceMoved)) break;
                }

            } else {
                // normal single-step diagonal move
                if (theBoard[toRow][toCol].isOccupied()) { System.out.println("Destination is occupied."); continue; }
                if (!validNormalMove(selected, fromRow, fromCol, toRow, toCol)) {
                    System.out.println("Invalid move for that piece type."); continue;
                }
                theBoard[toRow][toCol].addPiece(selected);
                theBoard[fromRow][fromCol].delete();
                selected.moved(toRow, toCol);
                lastPieceMoved = selected;
                checkingTheCrown(selected, toRow, toCol);
                moveMade = true;
            }

            if (moveMade && !gameOver) {
                switchTurns();
                printBoard();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Input helpers
    // -------------------------------------------------------------------------

    /** Prompts for "row col" input. Returns {-1,-1} if the user types 'resign', null on bad input. */
    private int[] promptCoords(String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        if (line.equalsIgnoreCase("resign")) return new int[]{-1, -1};
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Please enter two numbers separated by a space.");
            return null;
        }
        try {
            return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
        } catch (NumberFormatException e) {
            System.out.println("Invalid input — please enter numbers.");
            return null;
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROW && c >= 0 && c < COL;
    }

    private boolean validNormalMove(Pieces p, int fr, int fc, int tr, int tc) {
        if (Math.abs(tc - fc) != 1) return false;
        switch (p.getType()) {
            case RED:        return tr == fr + 1;
            case BLACK:      return tr == fr - 1;
            case RED_KING:
            case BLACK_KING: return Math.abs(tr - fr) == 1;
            default:         return false;
        }
    }

    private void resign(PlayerType side) {
        if (side == PlayerType.RED) { RED.lost();   System.out.println("Red resigned. Black wins!"); }
        else                        { BLACK.lost(); System.out.println("Black resigned. Red wins!"); }
    }
}
