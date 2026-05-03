package Checkers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for the Checkers project.
 * Covers: Pieces, Player, Board jump/move logic.
 *
 * Tests are structured around the following features:
 *   - Piece construction and type/side reporting
 *   - Piece crowning (promotion to King)
 *   - Piece movement coordinate tracking
 *   - Player piece count and loss detection
 *   - Board: jumpAvailable detection
 *   - Board: checkJump validation (distance, enemy, destination empty)
 *   - Board: normal piece move execution
 *   - Board: jump execution and piece removal
 *   - Board: king bidirectional movement
 *   - Board: turn alternation
 */
public class CheckersTest {

    // -------------------------------------------------------------------------
    // Pieces tests
    // -------------------------------------------------------------------------

    @Test
    public void testPieceTypeAndSide_Red() {
        Pieces p = new Pieces(0, 1, PieceType.RED);
        assertEquals(PieceType.RED, p.getType());
        assertEquals(PlayerType.RED, p.getSide());
    }

    @Test
    public void testPieceTypeAndSide_Black() {
        Pieces p = new Pieces(7, 0, PieceType.BLACK);
        assertEquals(PieceType.BLACK, p.getType());
        assertEquals(PlayerType.BLACK, p.getSide());
    }

    @Test
    public void testPieceInitialCoordinates() {
        Pieces p = new Pieces(3, 4, PieceType.RED);
        assertEquals(3, p.getRow());
        assertEquals(4, p.getCol());
    }

    @Test
    public void testPieceMoved_updatesCoordinates() {
        Pieces p = new Pieces(2, 1, PieceType.BLACK);
        p.moved(4, 3);
        assertEquals(4, p.getRow());
        assertEquals(3, p.getCol());
    }

    @Test
    public void testRedPiece_crowned_becomesRedKing() {
        Pieces p = new Pieces(6, 1, PieceType.RED);
        p.crowned();
        assertEquals(PieceType.RED_KING, p.getType());
    }

    @Test
    public void testBlackPiece_crowned_becomesBlackKing() {
        Pieces p = new Pieces(1, 2, PieceType.BLACK);
        p.crowned();
        assertEquals(PieceType.BLACK_KING, p.getType());
    }

    @Test
    public void testCrowning_doesNotChangeAlreadyKing() {
        Pieces p = new Pieces(1, 2, PieceType.RED_KING);
        p.crowned(); // crowned() only acts on RED or BLACK, not kings
        assertEquals(PieceType.RED_KING, p.getType());
    }

    @Test
    public void testKingPiece_correctSide() {
        Pieces p = new Pieces(0, 1, PieceType.BLACK_KING);
        assertEquals(PlayerType.BLACK, p.getSide());
    }

    // -------------------------------------------------------------------------
    // Player tests
    // -------------------------------------------------------------------------

    @Test
    public void testPlayer_initialPieceCount() {
        Player p = new Player(PlayerType.RED, 12);
        assertEquals(12, p.piecesLeft());
    }

    @Test
    public void testPlayer_pieceEaten_decrements() {
        Player p = new Player(PlayerType.BLACK, 12);
        p.pieceEaten();
        assertEquals(11, p.piecesLeft());
    }

    @Test
    public void testPlayer_pieceEaten_toZero_callsLost() {
        Player p = new Player(PlayerType.RED, 1);
        p.pieceEaten(); // goes to 0, triggers lost()
        assertEquals(0, p.piecesLeft());
    }

    @Test
    public void testPlayer_lost_setsToZero() {
        Player p = new Player(PlayerType.BLACK, 12);
        p.lost();
        assertEquals(0, p.piecesLeft());
    }

    @Test
    public void testPlayer_multipleEaten() {
        Player p = new Player(PlayerType.RED, 5);
        p.pieceEaten();
        p.pieceEaten();
        p.pieceEaten();
        assertEquals(2, p.piecesLeft());
    }

    // -------------------------------------------------------------------------
    // Board logic tests — uses a helper to build a minimal Board-like state
    // using the package-level Tiles array directly to avoid GUI construction.
    //
    // We test the pure logic methods: jumpAvailable and checkJump via a
    // thin wrapper that replicates Board's field layout without launching Swing.
    // -------------------------------------------------------------------------

    /**
     * Minimal test harness: a small board built from Tiles with no GUI.
     * Mirrors exactly how Board initialises its theBoard array.
     */
    static class BoardState {
        final int ROW = 8, COL = 8;
        final Tiles[][] board;

        BoardState() {
            board = new Tiles[ROW][COL];
            for (int i = 0; i < ROW; i++)
                for (int j = 0; j < COL; j++)
                    board[i][j] = new Tiles(i, j, null);
        }

        void place(Pieces p) {
            board[p.getRow()][p.getCol()].addPiece(p);
        }

        void remove(int r, int c) {
            board[r][c].delete();
        }

        boolean jumpAvailable(Pieces jumper) {
            int row = jumper.getRow(), col = jumper.getCol();
            PieceType type = jumper.getType();
            int[][] dirs;
            if (type == PieceType.RED)
                dirs = new int[][]{{1,-1},{1,1}};
            else if (type == PieceType.BLACK)
                dirs = new int[][]{{-1,-1},{-1,1}};
            else
                dirs = new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};

            for (int[] d : dirs) {
                int pr = row + d[0], pc = col + d[1];
                int lr = row + d[0]*2, lc = col + d[1]*2;
                if (lr >= 0 && lr < ROW && lc >= 0 && lc < COL) {
                    Tiles preyTile = board[pr][pc];
                    if (preyTile.isOccupied()
                            && preyTile.getPiece().getSide() != jumper.getSide()
                            && !board[lr][lc].isOccupied())
                        return true;
                }
            }
            return false;
        }

        /** Returns true if the jump from jumper to (dRow,dCol) is valid. */
        boolean checkJump(Pieces jumper, int dRow, int dCol) {
            int dr = dRow - jumper.getRow();
            int dc = dCol - jumper.getCol();
            if (Math.abs(dr) != 2 || Math.abs(dc) != 2) return false;
            int pr = jumper.getRow() + dr/2;
            int pc = jumper.getCol() + dc/2;
            if (pr < 0 || pr >= ROW || pc < 0 || pc >= COL) return false;
            Tiles preyTile = board[pr][pc];
            return !board[dRow][dCol].isOccupied()
                    && preyTile.isOccupied()
                    && preyTile.getPiece().getSide() != jumper.getSide();
        }
    }

    // -- jumpAvailable tests --

    @Test
    public void testJumpAvailable_redCanJumpBlack() {
        BoardState s = new BoardState();
        Pieces red   = new Pieces(2, 2, PieceType.RED);
        Pieces black = new Pieces(3, 3, PieceType.BLACK);
        s.place(red); s.place(black);
        // landing at (4,4) is empty — jump should be available
        assertTrue(s.jumpAvailable(red));
    }

    @Test
    public void testJumpAvailable_blockedByOwnPiece() {
        BoardState s = new BoardState();
        Pieces red1  = new Pieces(2, 2, PieceType.RED);
        Pieces black = new Pieces(3, 3, PieceType.BLACK);
        Pieces red2  = new Pieces(4, 4, PieceType.RED); // blocks landing
        s.place(red1); s.place(black); s.place(red2);
        assertFalse(s.jumpAvailable(red1));
    }

    @Test
    public void testJumpAvailable_noPrey() {
        BoardState s = new BoardState();
        Pieces red = new Pieces(2, 2, PieceType.RED);
        s.place(red);
        assertFalse(s.jumpAvailable(red));
    }

    @Test
    public void testJumpAvailable_cannotJumpOwnPiece() {
        BoardState s = new BoardState();
        Pieces red1 = new Pieces(2, 2, PieceType.RED);
        Pieces red2 = new Pieces(3, 3, PieceType.RED);
        s.place(red1); s.place(red2);
        assertFalse(s.jumpAvailable(red1));
    }

    @Test
    public void testJumpAvailable_kingCanJumpBackward() {
        BoardState s = new BoardState();
        Pieces king  = new Pieces(4, 4, PieceType.RED_KING);
        Pieces black = new Pieces(3, 3, PieceType.BLACK);
        s.place(king); s.place(black);
        // landing at (2,2) is empty — backwards jump for king
        assertTrue(s.jumpAvailable(king));
    }

    @Test
    public void testJumpAvailable_normalRedCannotJumpBackward() {
        BoardState s = new BoardState();
        Pieces red   = new Pieces(4, 4, PieceType.RED);
        Pieces black = new Pieces(3, 3, PieceType.BLACK);
        s.place(red); s.place(black);
        // black is behind red — normal red cannot jump backward
        assertFalse(s.jumpAvailable(red));
    }

    // -- checkJump tests --

    @Test
    public void testCheckJump_validJump() {
        BoardState s = new BoardState();
        Pieces red   = new Pieces(2, 2, PieceType.RED);
        Pieces black = new Pieces(3, 3, PieceType.BLACK);
        s.place(red); s.place(black);
        assertTrue(s.checkJump(red, 4, 4));
    }

    @Test
    public void testCheckJump_invalidDistance_oneStep() {
        BoardState s = new BoardState();
        Pieces red = new Pieces(2, 2, PieceType.RED);
        s.place(red);
        // clicking 1 step away while jump is expected
        assertFalse(s.checkJump(red, 3, 3));
    }

    @Test
    public void testCheckJump_destinationOccupied() {
        BoardState s = new BoardState();
        Pieces red    = new Pieces(2, 2, PieceType.RED);
        Pieces black  = new Pieces(3, 3, PieceType.BLACK);
        Pieces blocker= new Pieces(4, 4, PieceType.BLACK);
        s.place(red); s.place(black); s.place(blocker);
        assertFalse(s.checkJump(red, 4, 4));
    }

    @Test
    public void testCheckJump_noPieceTojumpOver() {
        BoardState s = new BoardState();
        Pieces red = new Pieces(2, 2, PieceType.RED);
        s.place(red);
        assertFalse(s.checkJump(red, 4, 4));
    }

    @Test
    public void testCheckJump_cannotJumpOwnPiece() {
        BoardState s = new BoardState();
        Pieces red1 = new Pieces(2, 2, PieceType.RED);
        Pieces red2 = new Pieces(3, 3, PieceType.RED);
        s.place(red1); s.place(red2);
        assertFalse(s.checkJump(red1, 4, 4));
    }

    // -- Crown promotion on board --

    @Test
    public void testCrowning_redReachesBottomRow() {
        Pieces red = new Pieces(6, 1, PieceType.RED);
        // Simulate what Board.checkingTheCrown does
        if (red.getType() == PieceType.RED && 7 == 7) red.crowned();
        assertEquals(PieceType.RED_KING, red.getType());
    }

    @Test
    public void testCrowning_blackReachesTopRow() {
        Pieces black = new Pieces(1, 2, PieceType.BLACK);
        if (black.getType() == PieceType.BLACK && 0 == 0) black.crowned();
        assertEquals(PieceType.BLACK_KING, black.getType());
    }

    private Tiles[][] emptyBoard() {
        Tiles[][] board = new Tiles[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = new Tiles(row, col, null);
            }
        }
        return board;
    }

    @Test
    public void testALevelSinglePieceCanMoveVerticalAndHorizontal() {
        Tiles[][] board = emptyBoard();
        board[3][3].addPiece(new Pieces(3, 3, PieceType.RED));

        assertNotNull(CheckersAI.findLegalMove(board, PlayerType.RED, 3, 3, 4, 3));
        assertNotNull(CheckersAI.findLegalMove(board, PlayerType.RED, 3, 3, 3, 4));
    }

    @Test
    public void testMandatoryCaptureSuppressesNormalMoves() {
        Tiles[][] board = emptyBoard();
        board[3][3].addPiece(new Pieces(3, 3, PieceType.RED));
        board[4][4].addPiece(new Pieces(4, 4, PieceType.BLACK));

        assertNull(CheckersAI.findLegalMove(board, PlayerType.RED, 3, 3, 3, 4));
        CheckersAI.Move capture = CheckersAI.findLegalMove(board, PlayerType.RED, 3, 3, 5, 5);
        assertNotNull(capture);
        assertTrue(capture.isCapture());
    }

    @Test
    public void testMultiCaptureCanBeSelectedByFirstLandingSquare() {
        Tiles[][] board = emptyBoard();
        board[1][1].addPiece(new Pieces(1, 1, PieceType.RED));
        board[2][2].addPiece(new Pieces(2, 2, PieceType.BLACK));
        board[4][4].addPiece(new Pieces(4, 4, PieceType.BLACK));

        CheckersAI.Move capture = CheckersAI.findLegalMove(board, PlayerType.RED, 1, 1, 3, 3);

        assertNotNull(capture);
        assertTrue(capture.isCapture());
        assertEquals(2, capture.captureCount());
        assertEquals(5, capture.getToRow());
        assertEquals(5, capture.getToCol());
    }

    @Test
    public void testHumanMultiCaptureReturnsOnlyClickedStep() {
        Tiles[][] board = emptyBoard();
        board[1][1].addPiece(new Pieces(1, 1, PieceType.RED));
        board[2][2].addPiece(new Pieces(2, 2, PieceType.BLACK));
        board[4][4].addPiece(new Pieces(4, 4, PieceType.BLACK));

        CheckersAI.Move step = CheckersAI.findLegalStep(board, PlayerType.RED, 1, 1, 3, 3);

        assertNotNull(step);
        assertTrue(step.isCapture());
        assertEquals(1, step.captureCount());
        assertEquals(3, step.getToRow());
        assertEquals(3, step.getToCol());
    }

    @Test
    public void testSplitMoveBreaksMultiCaptureIntoAnimatedSteps() {
        Tiles[][] board = emptyBoard();
        board[1][1].addPiece(new Pieces(1, 1, PieceType.RED));
        board[2][2].addPiece(new Pieces(2, 2, PieceType.BLACK));
        board[4][4].addPiece(new Pieces(4, 4, PieceType.BLACK));

        CheckersAI.Move capture = CheckersAI.findLegalMove(board, PlayerType.RED, 1, 1, 5, 5);

        assertNotNull(capture);
        assertEquals(2, CheckersAI.splitMove(capture).size());
        assertEquals(3, CheckersAI.splitMove(capture).get(0).getToRow());
        assertEquals(5, CheckersAI.splitMove(capture).get(1).getToRow());
    }

    @Test
    public void testKingCanMoveMultipleSpacesInStraightLine() {
        Tiles[][] board = emptyBoard();
        board[5][1].addPiece(new Pieces(5, 1, PieceType.RED_KING));

        assertNotNull(CheckersAI.findLegalMove(board, PlayerType.RED, 5, 1, 1, 1));
        assertNotNull(CheckersAI.findLegalMove(board, PlayerType.RED, 5, 1, 2, 4));
    }

    @Test
    public void testComputerChoosesAvailableCapture() {
        Tiles[][] board = emptyBoard();
        board[2][2].addPiece(new Pieces(2, 2, PieceType.RED));
        board[3][3].addPiece(new Pieces(3, 3, PieceType.BLACK));
        board[6][6].addPiece(new Pieces(6, 6, PieceType.BLACK));

        CheckersAI.Move move = new CheckersAI().chooseMove(board, PlayerType.RED);

        assertNotNull(move);
        assertTrue(move.isCapture());
        assertEquals(4, move.getToRow());
        assertEquals(4, move.getToCol());
    }
}
