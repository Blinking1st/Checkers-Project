package Checkers;

import java.util.List;
import java.util.Scanner;

/**
 * Text-based Checkers game. This view delegates A-level move legality and
 * computer move selection to CheckersAI so human and AI turns use the same rules.
 */
public class TextGame {
    private int ROW, COL, pieceRows, numPieces;
    private Tiles[][] theBoard;
    private Player RED, BLACK;
    private int turnCounter = 0;
    private int currentRow = 0;
    private int currentCol = 0;
    private boolean gameOver = false;
    private boolean playComputer = false;
    private boolean continuousJump = false;
    private PlayerType computerSide = PlayerType.RED;
    private final CheckersAI computerPlayer = new CheckersAI();
    private final Scanner scanner = new Scanner(System.in);

    public TextGame() {
        chooseBoardSize();
        chooseOpponent();
        initBoard();
        makePieces();
        RED = new Player(PlayerType.RED, numPieces);
        BLACK = new Player(PlayerType.BLACK, numPieces);
        gameLoop();
        scanner.close();
    }

    private void chooseBoardSize() {
        System.out.println("Select board size:");
        System.out.println("  1 - 8x8  (12 pieces each)");
        System.out.println("  2 - 10x10 (20 pieces each)");
        System.out.print("Enter choice (1 or 2): ");
        String input = scanner.nextLine().trim();
        if (input.equals("2")) {
            ROW = 10;
            COL = 10;
            pieceRows = 4;
            numPieces = 20;
            System.out.println("10x10 board selected.");
        } else {
            ROW = 8;
            COL = 8;
            pieceRows = 3;
            numPieces = 12;
            System.out.println("8x8 board selected.");
        }
    }

    private void chooseOpponent() {
        System.out.print("Play against the computer? (y/n): ");
        playComputer = scanner.nextLine().trim().equalsIgnoreCase("y");
        if (!playComputer) {
            return;
        }

        System.out.print("Choose your side (black/red). Black moves first: ");
        String side = scanner.nextLine().trim();
        computerSide = side.equalsIgnoreCase("red") ? PlayerType.BLACK : PlayerType.RED;
        System.out.println("Computer will play " + computerSide + ".");
    }

    private void initBoard() {
        theBoard = new Tiles[ROW][COL];
        for (int row = 0; row < ROW; row++) {
            for (int col = 0; col < COL; col++) {
                theBoard[row][col] = new Tiles(row, col, null);
            }
        }
    }

    private void makePieces() {
        for (int row = 0; row < ROW; row++) {
            for (int col = 0; col < COL; col++) {
                if ((row + col) % 2 == 1) {
                    if (row < pieceRows) {
                        theBoard[row][col].addPiece(new Pieces(row, col, PieceType.RED));
                    } else if (row >= ROW - pieceRows) {
                        theBoard[row][col].addPiece(new Pieces(row, col, PieceType.BLACK));
                    }
                }
            }
        }
    }

    private void gameLoop() {
        System.out.println("\nWelcome to A-level Checkers! Black moves first.");
        System.out.println("Single pieces move one space in any direction.");
        System.out.println("Kings move any clear distance in any direction.");
        System.out.println("Captures are mandatory. Enter coordinates as: row col");
        System.out.println("Type 'resign' at any prompt to concede.\n");
        printBoard();

        while (!gameOver) {
            PlayerType currentTurn = turn();
            checkNoLegalMoves(currentTurn);
            if (gameOver) {
                break;
            }

            System.out.println("=== " + currentTurn + "'s turn ===");
            if (playComputer && currentTurn == computerSide) {
                makeComputerMove(currentTurn);
            } else {
                makeHumanMove(currentTurn);
            }

            if (!gameOver) {
                if (!continuousJump) {
                    switchTurns();
                }
                printBoard();
            }
        }
    }

    private void makeHumanMove(PlayerType currentTurn) {
        int[] from = promptCoords("Select piece (row col): ");
        if (from == null) {
            return;
        }
        if (from[0] == -1) {
            resign(currentTurn);
            return;
        }
        if (!inBounds(from[0], from[1]) || !theBoard[from[0]][from[1]].isOccupied()) {
            System.out.println("No playable piece there.");
            return;
        }
        if (theBoard[from[0]][from[1]].getPiece().getSide() != currentTurn) {
            System.out.println("That is not your piece.");
            return;
        }
        if (continuousJump && (from[0] != currentRow || from[1] != currentCol)) {
            System.out.println("You must continue capturing with the same piece at "
                    + currentRow + " " + currentCol + ".");
            return;
        }

        int[] to = promptCoords("Select destination (row col): ");
        if (to == null) {
            return;
        }
        if (to[0] == -1) {
            resign(currentTurn);
            return;
        }
        if (!inBounds(to[0], to[1])) {
            System.out.println("That destination is not playable.");
            return;
        }

        CheckersAI.Move move = CheckersAI.findLegalStep(theBoard, currentTurn,
                from[0], from[1], to[0], to[1]);
        if (move == null) {
            System.out.println("Invalid move. If any capture is available, a capture must be taken.");
            return;
        }
        applyGameMove(move);
        if (move.isCapture() && CheckersAI.hasCaptureFrom(theBoard, currentTurn,
                move.getToRow(), move.getToCol())) {
            continuousJump = true;
            currentRow = move.getToRow();
            currentCol = move.getToCol();
            System.out.println("Another capture is available. Continue with the same piece.");
        } else {
            continuousJump = false;
        }
    }

    private void makeComputerMove(PlayerType currentTurn) {
        CheckersAI.Move move = computerPlayer.chooseMove(theBoard, currentTurn);
        if (move == null) {
            checkNoLegalMoves(currentTurn);
            return;
        }
        System.out.println("Computer moves from " + move.getFromRow() + " " + move.getFromCol()
                + " to " + move.getToRow() + " " + move.getToCol()
                + (move.isCapture() ? " and captures " + move.captureCount() + " piece(s)." : "."));
        List<CheckersAI.Move> steps = CheckersAI.splitMove(move);
        for (int i = 0; i < steps.size(); i++) {
            applyGameMove(steps.get(i));
            if (gameOver || i == steps.size() - 1) {
                return;
            }
            printBoard();
            pauseBetweenComputerCaptures();
        }
    }

    private void pauseBetweenComputerCaptures() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void applyGameMove(CheckersAI.Move move) {
        for (int[] captured : move.getCaptured()) {
            Pieces prey = theBoard[captured[0]][captured[1]].getPiece();
            if (prey != null && prey.getSide() == PlayerType.RED) {
                RED.pieceEaten();
            } else if (prey != null) {
                BLACK.pieceEaten();
            }
        }
        CheckersAI.applyMove(theBoard, move);
        checkWin();
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

    private void checkNoLegalMoves(PlayerType currentTurn) {
        if (CheckersAI.hasLegalMove(theBoard, currentTurn)) {
            return;
        }

        printBoard();
        System.out.println(currentTurn + " has no legal moves.");
        if (RED.piecesLeft() > BLACK.piecesLeft()) {
            System.out.println("Red wins by having more pieces on the board.");
        } else if (BLACK.piecesLeft() > RED.piecesLeft()) {
            System.out.println("Black wins by having more pieces on the board.");
        } else {
            System.out.println("The game is a draw by equal piece count.");
        }
        gameOver = true;
    }

    private PlayerType turn() {
        return turnCounter % 2 == 1 ? PlayerType.RED : PlayerType.BLACK;
    }

    private void switchTurns() {
        turnCounter++;
    }

    private int[] promptCoords(String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        if (line.equalsIgnoreCase("resign")) {
            return new int[]{-1, -1};
        }
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Please enter two numbers separated by a space.");
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter numbers.");
            return null;
        }
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < ROW && col >= 0 && col < COL;
    }

    private void resign(PlayerType side) {
        if (side == PlayerType.RED) {
            RED.lost();
            System.out.println("Red resigned. Black wins!");
        } else {
            BLACK.lost();
            System.out.println("Black resigned. Red wins!");
        }
        gameOver = true;
    }

    private void printBoard() {
        System.out.println();
        System.out.print("     ");
        for (int col = 0; col < COL; col++) {
            System.out.printf(" %-2d ", col);
        }
        System.out.println();
        printDivider();

        for (int row = 0; row < ROW; row++) {
            System.out.printf(" %2d |", row);
            for (int col = 0; col < COL; col++) {
                if (theBoard[row][col].isOccupied()) {
                    switch (theBoard[row][col].getPiece().getType()) {
                        case RED: System.out.print(" r |"); break;
                        case BLACK: System.out.print(" b |"); break;
                        case RED_KING: System.out.print(" R |"); break;
                        case BLACK_KING: System.out.print(" B |"); break;
                    }
                } else if ((row + col) % 2 == 0) {
                    System.out.print("###|");
                } else {
                    System.out.print(" . |");
                }
            }
            System.out.println();
            printDivider();
        }
        System.out.println("  r=Red  b=Black  R=Red King  B=Black King  . or ###=Available");
        System.out.println();
    }

    private void printDivider() {
        System.out.print("    +");
        for (int col = 0; col < COL; col++) {
            System.out.print("---+");
        }
        System.out.println();
    }
}
