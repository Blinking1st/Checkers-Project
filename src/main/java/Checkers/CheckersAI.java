package Checkers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates A-level legal moves and chooses computer moves with minimax.
 * The rule set here allows movement in all eight straight line directions,
 * requires captures when available, and searches four plies ahead.
 */
public class CheckersAI {
    public static final int LOOKAHEAD_DEPTH = 4;
    private static final long THINK_TIME_MILLIS = 200;
    private static final int QUIESCENCE_DEPTH = 2;

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    private static class SearchContext {
        private final long deadlineNanos;
        private final Map<String, Integer> transpositionTable = new HashMap<>();

        SearchContext(long thinkTimeMillis) {
            deadlineNanos = System.nanoTime() + (thinkTimeMillis * 1_000_000L);
        }

        boolean timeUp() {
            return System.nanoTime() >= deadlineNanos;
        }
    }

    public static class Move {
        private final ArrayList<int[]> path = new ArrayList<>();
        private final ArrayList<int[]> captured = new ArrayList<>();

        Move(int fromRow, int fromCol) {
            path.add(new int[]{fromRow, fromCol});
        }

        Move(Move other) {
            for (int[] step : other.path) {
                path.add(new int[]{step[0], step[1]});
            }
            for (int[] capture : other.captured) {
                captured.add(new int[]{capture[0], capture[1]});
            }
        }

        void addStep(int row, int col) {
            path.add(new int[]{row, col});
        }

        void addCapture(int row, int col) {
            captured.add(new int[]{row, col});
        }

        public int getFromRow() {
            return path.get(0)[0];
        }

        public int getFromCol() {
            return path.get(0)[1];
        }

        public int getToRow() {
            return path.get(path.size() - 1)[0];
        }

        public int getToCol() {
            return path.get(path.size() - 1)[1];
        }

        public boolean isCapture() {
            return !captured.isEmpty();
        }

        public int captureCount() {
            return captured.size();
        }

        public List<int[]> getPath() {
            return copyPositions(path);
        }

        public List<int[]> getCaptured() {
            return copyPositions(captured);
        }
    }

    /**
     * Chooses a move for the computer using iterative-deepening minimax.
     *
     * Instead of immediately attempting only a full four-ply search, the AI
     * searches depth 1, then depth 2, then depth 3, then depth 4. If the time
     * budget runs out during a deeper search, the method returns the best move
     * from the deepest fully or mostly completed pass. This keeps GUI play from
     * feeling frozen while still allowing the assignment-required four-ply
     * lookahead when the board position is small enough to finish in time.
     */
    public Move chooseMove(Tiles[][] board, PlayerType side) {
        List<Move> moves = orderedMoves(board, side);
        if (moves.isEmpty()) {
            return null;
        }

        Move bestMove = moves.get(0);
        SearchContext context = new SearchContext(THINK_TIME_MILLIS);

        for (int depth = 1; depth <= LOOKAHEAD_DEPTH; depth++) {
            int bestScoreThisDepth = Integer.MIN_VALUE;
            Move bestMoveThisDepth = bestMove;

            for (Move move : moves) {
                if (context.timeUp()) {
                    return bestMove;
                }

                Tiles[][] next = copyBoard(board);
                applyMove(next, move, true);
                int score = minimax(next, opponent(side), depth - 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE, side, context);
                if (score > bestScoreThisDepth) {
                    bestScoreThisDepth = score;
                    bestMoveThisDepth = move;
                }
            }

            bestMove = bestMoveThisDepth;
        }
        return bestMove;
    }

    /**
     * Returns all legal moves for a side under the A-level rules.
     *
     * The important rule enforced here is mandatory capture: if any piece on
     * the current side can capture, non-capturing moves are removed from the
     * result. Keeping that rule in one method makes the human players and AI
     * use exactly the same legal move list.
     */
    public static List<Move> getLegalMoves(Tiles[][] board, PlayerType side) {
        ArrayList<Move> captures = new ArrayList<>();
        ArrayList<Move> normalMoves = new ArrayList<>();

        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (!board[row][col].isOccupied()) {
                    continue;
                }
                Pieces piece = board[row][col].getPiece();
                if (piece.getSide() != side) {
                    continue;
                }
                captures.addAll(getCaptureMoves(board, row, col));
                normalMoves.addAll(getNormalMoves(board, row, col));
            }
        }
        return captures.isEmpty() ? normalMoves : captures;
    }

    /**
     * Finds the user's requested move inside the generated legal move list.
     *
     * GUI and text mode both call this instead of reimplementing movement
     * checks. That way A-level movement, mandatory captures, and king movement
     * stay consistent between human turns and computer turns.
     */
    public static Move findLegalMove(Tiles[][] board, PlayerType side,
                                     int fromRow, int fromCol, int toRow, int toCol) {
        for (Move move : getLegalMoves(board, side)) {
            if (move.getFromRow() == fromRow && move.getFromCol() == fromCol
                    && matchesRequestedDestination(move, toRow, toCol)) {
                return move;
            }
        }
        return null;
    }

    /**
     * Finds only the next step requested by a human player.
     *
     * Minimax wants a full turn move, including every forced capture in a
     * chain. Human players expect to click each jump one at a time, so this
     * method slices a full legal capture path at the clicked landing square.
     */
    public static Move findLegalStep(Tiles[][] board, PlayerType side,
                                     int fromRow, int fromCol, int toRow, int toCol) {
        for (Move move : getLegalMoves(board, side)) {
            if (move.getFromRow() != fromRow || move.getFromCol() != fromCol) {
                continue;
            }
            if (!move.isCapture()) {
                if (move.getToRow() == toRow && move.getToCol() == toCol) {
                    return move;
                }
                continue;
            }
            Move partial = partialMoveThrough(move, toRow, toCol);
            if (partial != null) {
                return partial;
            }
        }
        return null;
    }

    /** Returns true when the chosen piece still has a capture after a jump. */
    public static boolean hasCaptureFrom(Tiles[][] board, PlayerType side, int row, int col) {
        if (!inBounds(board, row, col) || !board[row][col].isOccupied()
                || board[row][col].getPiece().getSide() != side) {
            return false;
        }
        return !getCaptureMoves(board, row, col).isEmpty();
    }

    /**
     * Breaks a full turn move into individual board updates.
     *
     * GUI animation uses these one-step moves so a multi-capture is visible as
     * a short sequence. The AI still chooses the original full move.
     */
    public static List<Move> splitMove(Move move) {
        ArrayList<Move> steps = new ArrayList<>();
        if (!move.isCapture()) {
            steps.add(new Move(move));
            return steps;
        }
        for (int i = 1; i < move.path.size(); i++) {
            int[] from = move.path.get(i - 1);
            int[] to = move.path.get(i);
            Move step = new Move(from[0], from[1]);
            step.addCapture(move.captured.get(i - 1)[0], move.captured.get(i - 1)[1]);
            step.addStep(to[0], to[1]);
            steps.add(step);
        }
        return steps;
    }

    public static boolean hasLegalMove(Tiles[][] board, PlayerType side) {
        return !getLegalMoves(board, side).isEmpty();
    }

    public static PlayerType opponent(PlayerType side) {
        return side == PlayerType.RED ? PlayerType.BLACK : PlayerType.RED;
    }

    /**
     * Applies one real move to a board.
     *
     * Public callers use the non-silent version for actual game play. The
     * minimax search uses the private silent overload so simulated future moves
     * do not flood the console with "Moved to x,y" debug output.
     */
    public static void applyMove(Tiles[][] board, Move move) {
        applyMove(board, move, false);
    }

    private static void applyMove(Tiles[][] board, Move move, boolean silent) {
        Pieces piece = board[move.getFromRow()][move.getFromCol()].getPiece();
        board[move.getFromRow()][move.getFromCol()].delete();

        for (int[] captured : move.captured) {
            board[captured[0]][captured[1]].delete();
        }

        int toRow = move.getToRow();
        int toCol = move.getToCol();
        if (silent) {
            piece.movedSilently(toRow, toCol);
        } else {
            piece.moved(toRow, toCol);
        }
        if (piece.getType() == PieceType.RED && toRow == board.length - 1) {
            crown(piece, silent);
        } else if (piece.getType() == PieceType.BLACK && toRow == 0) {
            crown(piece, silent);
        }
        board[toRow][toCol].addPiece(piece);
    }

    /**
     * Copies the board into fresh Tiles and Pieces objects.
     *
     * Minimax explores hypothetical futures by copying the board and applying
     * moves to the copy. This keeps search from mutating the real GUI/text game
     * state while it evaluates possible continuations.
     */
    public static Tiles[][] copyBoard(Tiles[][] source) {
        Tiles[][] copy = new Tiles[source.length][source[0].length];
        for (int row = 0; row < source.length; row++) {
            for (int col = 0; col < source[row].length; col++) {
                copy[row][col] = new Tiles(row, col, null);
                if (source[row][col].isOccupied()) {
                    Pieces p = source[row][col].getPiece();
                    copy[row][col].addPiece(new Pieces(row, col, p.getType()));
                }
            }
        }
        return copy;
    }

    /**
     * Alpha-beta minimax search with a transposition cache.
     *
     * Alpha-beta avoids searching branches that cannot affect the final choice.
     * This is much faster when good moves are searched first, so this method
     * always uses orderedMoves(). The transposition table stores already-seen
     * board positions at a given depth, which avoids repeating work when
     * different move orders reach the same or equivalent position.
     */
    private int minimax(Tiles[][] board, PlayerType currentSide, int depth,
                        int alpha, int beta, PlayerType maximizingSide,
                        SearchContext context) {
        if (context.timeUp()) {
            return evaluate(board, maximizingSide);
        }

        String cacheKey = boardKey(board, currentSide, maximizingSide, depth);
        Integer cachedScore = context.transpositionTable.get(cacheKey);
        if (cachedScore != null) {
            return cachedScore;
        }

        List<Move> moves = orderedMoves(board, currentSide);
        if (depth == 0 || moves.isEmpty()) {
            int score = quiescence(board, currentSide, alpha, beta,
                    maximizingSide, QUIESCENCE_DEPTH, context);
            context.transpositionTable.put(cacheKey, score);
            return score;
        }

        boolean searchedAllMoves = true;
        int best;
        if (currentSide == maximizingSide) {
            best = Integer.MIN_VALUE;
            for (Move move : moves) {
                if (context.timeUp()) {
                    searchedAllMoves = false;
                    break;
                }
                Tiles[][] next = copyBoard(board);
                applyMove(next, move, true);
                best = Math.max(best, minimax(next, opponent(currentSide), depth - 1,
                        alpha, beta, maximizingSide, context));
                alpha = Math.max(alpha, best);
                if (beta <= alpha) {
                    searchedAllMoves = false;
                    break;
                }
            }
        } else {
            best = Integer.MAX_VALUE;
            for (Move move : moves) {
                if (context.timeUp()) {
                    searchedAllMoves = false;
                    break;
                }
                Tiles[][] next = copyBoard(board);
                applyMove(next, move, true);
                best = Math.min(best, minimax(next, opponent(currentSide), depth - 1,
                        alpha, beta, maximizingSide, context));
                beta = Math.min(beta, best);
                if (beta <= alpha) {
                    searchedAllMoves = false;
                    break;
                }
            }
        }

        if (best == Integer.MIN_VALUE || best == Integer.MAX_VALUE) {
            return evaluate(board, maximizingSide);
        }
        if (searchedAllMoves) {
            context.transpositionTable.put(cacheKey, best);
        }
        return best;
    }

    /**
     * Extends leaf evaluation through short forced-capture sequences.
     *
     * Plain minimax can stop exactly before a capture and misjudge the board.
     * Quiescence search reduces that problem by searching only noisy tactical
     * moves, currently captures, for a very small extra depth. This improves
     * play without exploding the search as much as a full extra minimax ply.
     */
    private int quiescence(Tiles[][] board, PlayerType currentSide, int alpha, int beta,
                           PlayerType maximizingSide, int depth, SearchContext context) {
        int standPat = evaluate(board, maximizingSide);
        if (depth == 0 || context.timeUp()) {
            return standPat;
        }

        List<Move> captures = new ArrayList<>();
        for (Move move : orderedMoves(board, currentSide)) {
            if (move.isCapture()) {
                captures.add(move);
            }
        }
        if (captures.isEmpty()) {
            return standPat;
        }

        if (currentSide == maximizingSide) {
            int best = standPat;
            alpha = Math.max(alpha, best);
            for (Move move : captures) {
                if (context.timeUp() || beta <= alpha) {
                    break;
                }
                Tiles[][] next = copyBoard(board);
                applyMove(next, move, true);
                best = Math.max(best, quiescence(next, opponent(currentSide),
                        alpha, beta, maximizingSide, depth - 1, context));
                alpha = Math.max(alpha, best);
            }
            return best;
        } else {
            int best = standPat;
            beta = Math.min(beta, best);
            for (Move move : captures) {
                if (context.timeUp() || beta <= alpha) {
                    break;
                }
                Tiles[][] next = copyBoard(board);
                applyMove(next, move, true);
                best = Math.min(best, quiescence(next, opponent(currentSide),
                        alpha, beta, maximizingSide, depth - 1, context));
                beta = Math.min(beta, best);
            }
            return best;
        }
    }

    /**
     * Scores a board from one side's point of view.
     *
     * Material is the largest factor, kings are worth more than single pieces,
     * uncrowned pieces get a small bonus for advancing toward promotion, and
     * mobility is rewarded lightly so the AI avoids boxed-in positions.
     */
    private int evaluate(Tiles[][] board, PlayerType side) {
        int score = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (!board[row][col].isOccupied()) {
                    continue;
                }
                Pieces piece = board[row][col].getPiece();
                int value = isKing(piece) ? 175 : 100;
                if (piece.getType() == PieceType.RED) {
                    value += row * 4;
                } else if (piece.getType() == PieceType.BLACK) {
                    value += (board.length - 1 - row) * 4;
                }
                if (piece.getSide() == side) {
                    score += value;
                } else {
                    score -= value;
                }
            }
        }
        score += getLegalMoves(board, side).size() * 3;
        score -= getLegalMoves(board, opponent(side)).size() * 3;
        return score;
    }

    /**
     * Sorts legal moves so alpha-beta can prune more aggressively.
     *
     * Alpha-beta is only dramatically faster when strong moves are considered
     * early. This ordering tries captures first, then larger captures, then
     * promotions, then moves made by kings. The search result is still minimax;
     * this only changes the order in which equivalent candidates are explored.
     */
    private static List<Move> orderedMoves(Tiles[][] board, PlayerType side) {
        ArrayList<Move> moves = new ArrayList<>(getLegalMoves(board, side));
        moves.sort(Comparator.comparingInt((Move move) -> moveOrderingScore(board, move)).reversed());
        return moves;
    }

    /**
     * Gives a quick tactical priority to a move for move ordering.
     *
     * This score is not the final board evaluation. It is intentionally cheap:
     * captures, multi-captures, promotions, and king moves are searched early
     * because they are more likely to affect alpha-beta cutoffs.
     */
    private static int moveOrderingScore(Tiles[][] board, Move move) {
        Pieces piece = board[move.getFromRow()][move.getFromCol()].getPiece();
        int score = 0;
        if (move.isCapture()) {
            score += 10_000 + (move.captureCount() * 1_000);
        }
        if (piece != null && promotes(piece, board, move)) {
            score += 750;
        }
        if (piece != null && isKing(piece)) {
            score += 250;
        }
        score += centerBonus(board, move.getToRow(), move.getToCol());
        return score;
    }

    /**
     * Builds a compact board key for the transposition table.
     *
     * The key includes board contents, side to move, maximizing side, and
     * remaining depth. Including depth matters because a position searched to
     * depth 1 is less reliable than the same position searched to depth 4.
     */
    private static String boardKey(Tiles[][] board, PlayerType currentSide,
                                   PlayerType maximizingSide, int depth) {
        StringBuilder key = new StringBuilder(board.length * board[0].length + 12);
        key.append(currentSide).append('|').append(maximizingSide).append('|').append(depth).append('|');
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (!board[row][col].isOccupied()) {
                    key.append('.');
                } else {
                    switch (board[row][col].getPiece().getType()) {
                        case RED: key.append('r'); break;
                        case BLACK: key.append('b'); break;
                        case RED_KING: key.append('R'); break;
                        case BLACK_KING: key.append('B'); break;
                    }
                }
            }
        }
        return key.toString();
    }

    /**
     * Generates non-capturing moves for a piece under the A-level movement rules.
     *
     * Single pieces move exactly one open square in any of the eight directions.
     * Kings move any clear distance in any of the eight directions.
     */
    private static List<Move> getNormalMoves(Tiles[][] board, int row, int col) {
        ArrayList<Move> moves = new ArrayList<>();
        Pieces piece = board[row][col].getPiece();
        for (int[] dir : DIRECTIONS) {
            if (isKing(piece)) {
                int nextRow = row + dir[0];
                int nextCol = col + dir[1];
                while (inBounds(board, nextRow, nextCol)
                        && !board[nextRow][nextCol].isOccupied()) {
                    Move move = new Move(row, col);
                    move.addStep(nextRow, nextCol);
                    moves.add(move);
                    nextRow += dir[0];
                    nextCol += dir[1];
                }
            } else {
                int nextRow = row + dir[0];
                int nextCol = col + dir[1];
                if (inBounds(board, nextRow, nextCol)
                        && !board[nextRow][nextCol].isOccupied()) {
                    Move move = new Move(row, col);
                    move.addStep(nextRow, nextCol);
                    moves.add(move);
                }
            }
        }
        return moves;
    }

    /**
     * Generates all complete capture paths for one piece.
     *
     * A capture may chain into another capture. This method returns only
     * complete paths, so callers can apply one Move and get the full capture
     * sequence without asking the user or AI for intermediate jump squares.
     */
    private static List<Move> getCaptureMoves(Tiles[][] board, int row, int col) {
        ArrayList<Move> moves = new ArrayList<>();
        Pieces piece = board[row][col].getPiece();
        Move start = new Move(row, col);
        collectCaptures(board, piece, row, col, start, moves);
        return moves;
    }

    /**
     * Recursively follows capture chains for one moving piece.
     *
     * Each recursive step copies the board, applies a silent single jump, and
     * looks for more captures from the landing square. When no further capture
     * exists, the accumulated path is added as a complete legal capture move.
     */
    private static void collectCaptures(Tiles[][] board, Pieces piece, int row, int col,
                                        Move current, ArrayList<Move> moves) {
        boolean foundFurtherCapture = false;
        for (Move jump : singleCaptures(board, piece, row, col)) {
            foundFurtherCapture = true;
            Tiles[][] next = copyBoard(board);
            Move extended = new Move(current);
            extended.addCapture(jump.captured.get(0)[0], jump.captured.get(0)[1]);
            extended.addStep(jump.getToRow(), jump.getToCol());
            applySingleJump(next, row, col, jump.getToRow(), jump.getToCol(),
                    jump.captured.get(0)[0], jump.captured.get(0)[1]);
            Pieces moved = next[jump.getToRow()][jump.getToCol()].getPiece();
            collectCaptures(next, moved, jump.getToRow(), jump.getToCol(), extended, moves);
        }
        if (!foundFurtherCapture && current.isCapture()) {
            moves.add(current);
        }
    }

    /**
     * Returns the one-jump captures available from a piece's current square.
     *
     * Single pieces jump exactly two squares over an adjacent opponent. Kings
     * scan along a direction, find one opponent, and may land on any empty
     * square beyond that opponent in the same line.
     */
    private static List<Move> singleCaptures(Tiles[][] board, Pieces piece, int row, int col) {
        ArrayList<Move> moves = new ArrayList<>();
        for (int[] dir : DIRECTIONS) {
            if (isKing(piece)) {
                addKingCaptures(board, piece, row, col, dir, moves);
            } else {
                int preyRow = row + dir[0];
                int preyCol = col + dir[1];
                int landRow = row + (dir[0] * 2);
                int landCol = col + (dir[1] * 2);
                if (inBounds(board, landRow, landCol)
                        && board[preyRow][preyCol].isOccupied()
                        && board[preyRow][preyCol].getPiece().getSide() != piece.getSide()
                        && !board[landRow][landCol].isOccupied()) {
                    Move move = new Move(row, col);
                    move.addCapture(preyRow, preyCol);
                    move.addStep(landRow, landCol);
                    moves.add(move);
                }
            }
        }
        return moves;
    }

    /**
     * Adds king captures in one direction.
     *
     * A king can travel through empty squares, jump exactly one opponent in
     * that line, and land on any open square beyond it. A same-side piece or a
     * second opponent before landing blocks the capture in that direction.
     */
    private static void addKingCaptures(Tiles[][] board, Pieces piece, int row, int col,
                                        int[] dir, ArrayList<Move> moves) {
        int scanRow = row + dir[0];
        int scanCol = col + dir[1];
        int preyRow = -1;
        int preyCol = -1;

        while (inBounds(board, scanRow, scanCol)) {
            if (board[scanRow][scanCol].isOccupied()) {
                Pieces scanned = board[scanRow][scanCol].getPiece();
                if (scanned.getSide() == piece.getSide() || preyRow != -1) {
                    return;
                }
                preyRow = scanRow;
                preyCol = scanCol;
            } else if (preyRow != -1) {
                Move move = new Move(row, col);
                move.addCapture(preyRow, preyCol);
                move.addStep(scanRow, scanCol);
                moves.add(move);
            }
            scanRow += dir[0];
            scanCol += dir[1];
        }
    }

    /**
     * Applies one silent jump while building multi-capture paths.
     *
     * This is only used on copied boards during move generation, so it never
     * updates player piece counts or prints debug text.
     */
    private static void applySingleJump(Tiles[][] board, int fromRow, int fromCol,
                                        int toRow, int toCol, int preyRow, int preyCol) {
        Pieces piece = board[fromRow][fromCol].getPiece();
        board[fromRow][fromCol].delete();
        board[preyRow][preyCol].delete();
        piece.movedSilently(toRow, toCol);
        board[toRow][toCol].addPiece(piece);
    }

    /**
     * Crowns a piece using either the visible or silent path.
     *
     * Real game moves may print the existing crown message; search simulations
     * use the silent path to avoid console spam while minimax is thinking.
     */
    private static void crown(Pieces piece, boolean silent) {
        if (silent) {
            piece.crownedSilently();
        } else {
            piece.crowned();
        }
    }

    private static boolean promotes(Pieces piece, Tiles[][] board, Move move) {
        return (piece.getType() == PieceType.RED && move.getToRow() == board.length - 1)
                || (piece.getType() == PieceType.BLACK && move.getToRow() == 0);
    }

    private static boolean matchesRequestedDestination(Move move, int row, int col) {
        if (move.getToRow() == row && move.getToCol() == col) {
            return true;
        }
        if (!move.isCapture()) {
            return false;
        }
        for (int i = 1; i < move.path.size(); i++) {
            int[] step = move.path.get(i);
            if (step[0] == row && step[1] == col) {
                return true;
            }
        }
        return false;
    }

    private static Move partialMoveThrough(Move move, int row, int col) {
        int[] firstLanding = move.path.get(1);
        if (firstLanding[0] == row && firstLanding[1] == col) {
            Move partial = new Move(move.getFromRow(), move.getFromCol());
            int[] captured = move.captured.get(0);
            partial.addCapture(captured[0], captured[1]);
            partial.addStep(row, col);
            return partial;
        }
        return null;
    }

    private static int centerBonus(Tiles[][] board, int row, int col) {
        int rowCenter = board.length / 2;
        int colCenter = board[row].length / 2;
        return Math.max(0, 20 - (Math.abs(row - rowCenter) + Math.abs(col - colCenter)));
    }

    private static boolean inBounds(Tiles[][] board, int row, int col) {
        return row >= 0 && row < board.length && col >= 0 && col < board[row].length;
    }

    private static boolean isKing(Pieces piece) {
        return piece.getType() == PieceType.RED_KING || piece.getType() == PieceType.BLACK_KING;
    }

    private static List<int[]> copyPositions(List<int[]> positions) {
        ArrayList<int[]> copy = new ArrayList<>();
        for (int[] position : positions) {
            copy.add(new int[]{position[0], position[1]});
        }
        return copy;
    }
}
