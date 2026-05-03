package Checkers;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Board extends JComponent {
	// Numerous private variables with different characteristics
	private int COL;
	private int ROW;
	private int pieceRows;  // number of rows each side occupies at start
	private int numPieces;  // starting piece count per player
	private Tiles theBoard[][];
	private Tiles[][] tile;
	private JFrame frame;
	private JPanel panel;
	private JMenuBar menuBar;
	private JMenu menu; // Menu
	private JMenuItem help;
	private JMenuItem resign; // Resign option
	private int destRow = 0;
	private int destCol = 0;
	private int currentRow = 0;
	private int currentCol = 0;
	private int preyRow = 0; // Coordinates of piece being eaten
	private int preyCol = 0;
	private int turnCounter = 0;
	private Pieces lastPieceMoved; // Proposed for a path-finding function...
	private Player RED; // Players of the game
	private Player BLACK;
	private String loser; // Prints losing side
	private boolean continuousJump = false; // True when player must continue jumping same piece
	private boolean playComputer = false;
	private boolean computerThinking = false;
	private PlayerType computerSide = PlayerType.RED;
	private CheckersAI computerPlayer = new CheckersAI();

	/**
	 * Constructor for the Board class. Creates the board and all components, and
	 * makes the frame visible.
	 */
	public Board() {
		chooseBoardSize();
		createComponents();
		addingTiles();
		makePieces();
		createMenu();
		BLACK = new Player(PlayerType.BLACK, numPieces);
		RED = new Player(PlayerType.RED, numPieces);
		chooseOpponent();
		frame.add(panel);
		frame.setVisible(true);
		maybeStartComputerTurn();
	}

	/**
	 * Shows a dialog at startup letting the player choose between an 8x8 board
	 * (12 pieces each) or a 10x10 board (20 pieces each), then sets ROW, COL,
	 * pieceRows, and numPieces accordingly and allocates the board arrays.
	 */
	private void chooseBoardSize() {
		Object[] options = {"8x8 (12 pieces each)", "10x10 (20 pieces each)"};
		int choice = JOptionPane.showOptionDialog(
				null,
				"Select a board size to play on:",
				"Checkers - Board Size",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]);
		if (choice == 1) {
			ROW = 10;
			COL = 10;
			pieceRows = 4;
			numPieces = 20;
		} else {
			// Default to 8x8 (also handles dialog closed/cancelled)
			ROW = 8;
			COL = 8;
			pieceRows = 3;
			numPieces = 12;
		}
		theBoard = new Tiles[ROW][COL];
		tile = new Tiles[ROW][COL];
	}

	private void chooseOpponent() {
		Object[] options = {"Two players", "Play computer"};
		int choice = JOptionPane.showOptionDialog(
				frame,
				"Choose the GUI game type:",
				"Checkers - Opponent",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]);
		playComputer = choice == 1;
		if (!playComputer) {
			return;
		}

		Object[] sides = {"Black", "Red"};
		int sideChoice = JOptionPane.showOptionDialog(
				frame,
				"Choose your side. Black moves first:",
				"Checkers - Side",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				sides,
				sides[0]);
		computerSide = sideChoice == 1 ? PlayerType.BLACK : PlayerType.RED;
	}

	/**
	 * Checks if either side has won the game. If one side has no pieces left, the other side wins. 
	 */
	public void checkWin() { // If one side has no pieces left, the other side
		// wins. No draw game functionality
		if (RED.piecesLeft() == 0) {
			loser = "Red";
			displayDialog();
			frame.dispose();
		} else if (BLACK.piecesLeft() == 0) {
			loser = "Black";
			displayDialog();
			frame.dispose();
		}
	}

	public boolean isComputerTurn() {
		return playComputer && turn() == computerSide;
	}

	private void maybeStartComputerTurn() {
		if (!playComputer || computerThinking || continuousJump
				|| turn() != computerSide || frame == null || !frame.isDisplayable()) {
			return;
		}

		computerThinking = true;
		Timer timer = new Timer(300, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				makeComputerMove();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	private void makeComputerMove() {
		if (frame == null || !frame.isDisplayable()) {
			return;
		}

		CheckersAI.Move move = computerPlayer.chooseMove(theBoard, computerSide);
		if (move == null) {
			JOptionPane.showMessageDialog(frame,
					computerSide + " has no legal moves. "
							+ CheckersAI.opponent(computerSide) + " wins!");
			frame.dispose();
			computerThinking = false;
			return;
		}

		List<CheckersAI.Move> steps = CheckersAI.splitMove(move);
		if (steps.size() > 1) {
			animateComputerMove(steps, 0);
		} else {
			applyBoardMove(move);
			finishComputerTurn();
		}
	}

	private void animateComputerMove(List<CheckersAI.Move> steps, int index) {
		if (frame == null || !frame.isDisplayable()) {
			computerThinking = false;
			return;
		}

		applyBoardMove(steps.get(index));
		panel.repaint();
		if (!frame.isDisplayable()) {
			computerThinking = false;
			return;
		}
		if (index == steps.size() - 1) {
			finishComputerTurn();
			return;
		}

		Timer timer = new Timer(500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				animateComputerMove(steps, index + 1);
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	private void finishComputerTurn() {
		if (frame != null && frame.isDisplayable()) {
			switchTurns();
			panel.repaint();
		}
		computerThinking = false;
	}

	private void applyBoardMove(CheckersAI.Move move) {
		for (int[] captured : move.getCaptured()) {
			Pieces prey = theBoard[captured[0]][captured[1]].getPiece();
			if (prey != null && prey.getSide() == PlayerType.RED) {
				RED.pieceEaten();
			} else if (prey != null) {
				BLACK.pieceEaten();
			}
		}

		CheckersAI.applyMove(theBoard, move);
		lastPieceMoved = theBoard[move.getToRow()][move.getToCol()].getPiece();
		currentRow = move.getToRow();
		currentCol = move.getToCol();
		checkWin();
	}

	/**
	 * Switches turns after a move is made. Increments the turn counter, which is used to determine which player's turn it is.
	 */
	public void switchTurns() { // Once a move is made, switch/increment turns
		turnCounter++;
	}

	/**
	 * Determines which player's turn it is based on the turn counter. If the turn counter is odd, it's Red's turn; if it's even, it's
	 * @return the PlayerType of the current player.
	 */
	public PlayerType turn() { // Makes sure turns are alternating. Black goes
		// first.
		if (turnCounter % 2 == 1) {
			return PlayerType.RED;
		} else {
			return PlayerType.BLACK;
		}
	}

/**
 * Gets the row and column of the piece that is being moved. This method is called when a piece is clicked, and it updates the currentRow and currentCol variables with the coordinates of the clicked piece. These coordinates are then used to determine potential moves and to execute the move when the destination is selected.
 * @param row the row of the clicked piece
 * @param col the column of the clicked piece
 */
	public void getRootRowCol(int row, int col) { // Passes in clicked
		// piece/tile coordinates
		currentRow = row;
		currentCol = col;
	}
	/**
	 * Creates the main components of the game, including the JFrame and JPanel. The JFrame is set to a specific size, made non-resizable, and configured to close the application when the window is closed. The JPanel is initialized with a GridLayout to hold the tiles of the checkerboard.
	 */
	public void createComponents() { // Creation of JComponents
		frame = new JFrame();
		int tileSize = 75;
		frame.setSize(new Dimension(COL * tileSize, ROW * tileSize + 23));
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel = new JPanel(new GridLayout(ROW, COL));
	}

	/**
	 * Creates the tiles for the checkerboard and adds them to the panel. Each tile is initialized with its row and column coordinates, and a MouseListener is added to each tile to handle click events. The tiles are stored in a 2D array for easy access when moving pieces.
	 */
	public void addingTiles() { // Creation and storage of Tiles. Tiles extends
		// JPanel
		Mouse m = new Mouse();
		for (int i = 0; i < ROW; i++) {
			for (int j = 0; j < COL; j++) {
				tile[i][j] = new Tiles(i, j, this); // Tile(row, column, Board
				// this);
				theBoard[i][j] = tile[i][j];
				theBoard[i][j].addMouseListener(m);
				panel.add(theBoard[i][j]); // i is row, j is column
			}
		}
	}

	/**
	 * Creates the pieces for both players and places them on the board. Red pieces are placed on the top three rows, while black pieces are placed on the bottom three rows. Each piece is initialized with its row, column, and type (Red or Black), and added to the corresponding tile on the board.
	 */
	public void makePieces() { // Creation and storage of Pieces
		for (int i = 0; i < ROW; i++) {
			for (int j = 0; j < COL; j++) {
				if ((i + j) % 2 == 1) {
					if (i < pieceRows) {
						theBoard[i][j].addPiece(new Pieces(i, j, PieceType.RED));
					} else if (i >= ROW - pieceRows) {
						theBoard[i][j].addPiece(new Pieces(i, j, PieceType.BLACK));
					}
				}
			}
		}
	}
	/**
	 * Checks if a piece has reached the opposite end of the board and should be crowned as a King. If a Red piece reaches row 7, it is crowned; if a Black piece reaches row 0, it is crowned. This method is called after a piece is moved to ensure that any piece that qualifies for crowning is updated accordingly.
	 * @param p the piece being moved
	 * @param destRow the destination row of the piece
	 * @param destCol the destination column of the piece
	 */
	public void checkingTheCrown(Pieces p, int destRow, int destCol) { // Checks
		// if a
		// piece
		// has
		// qualified
		// to
		// become
		// a
		// King
		if (p.getType() == PieceType.RED && destRow == ROW - 1) {
			p.crowned();
		} else if (p.getType() == PieceType.BLACK && destRow == 0) {
			p.crowned();
		}
	}

	/**
	 * Retrieves the tile at the specified coordinates. This method is used to access the tile on the board when a piece is being moved or when checking for potential moves. It ensures that the coordinates are within the bounds of the board before returning the tile; if the coordinates are out of bounds, it returns null.
	 * @param xCoord the row coordinate of the tile
	 * @param yCoord the column coordinate of the tile
	 * @return the tile at the specified coordinates, or null if the coordinates are out of bounds
	 */
	public Tiles getTile(int xCoord, int yCoord) {
		if ((xCoord >= 0 && xCoord <= ROW - 1) && (yCoord >= 0 && yCoord <= COL - 1)) {
			return theBoard[xCoord][yCoord];
		} else
			return null;
	}
	/**
	 * Returns the current turn count. This method can be used to determine how many turns have been taken in the game, which can be useful for tracking game progress or implementing features that depend on the number of turns (e.g., a timer or move limit).
	 * @return the current turn count
	 */
	public int returnTurns() {
		return turnCounter;
	}

	/** Returns whether the current player is locked into continuing a multi-jump. */
	public boolean isContinuousJump() {
		return continuousJump;
	}

	/** Returns the last piece that was moved (used to enforce multi-jump). */
	public Pieces getLastPieceMoved() {
		return lastPieceMoved;
	}

	/**
	 * Checks if a piece can perform a jump move. This method calculates the potential jump move based on the current position of the piece and the destination coordinates. It checks if the destination tile is unoccupied and if there is an opponent's piece (the "prey") in the correct position to be jumped over. If both conditions are met, it returns true, indicating that the jump is valid; otherwise, it returns false and switches turns.
	 * @param jumper the piece that is attempting to jump
	 * @return true if the jump is valid, false otherwise
	 */
	public boolean checkJump(Pieces jumper) {
		int rowDistance = destRow - jumper.getRow();
		int colDistance = destCol - jumper.getCol();
		// A valid jump is exactly 2 steps diagonally
		if (Math.abs(rowDistance) != 2 || Math.abs(colDistance) != 2) {
			System.err.println("Not a valid jump distance to " + destRow + "," + destCol);
			return false;
		}
		preyRow = jumper.getRow() + (rowDistance / 2);
		preyCol = jumper.getCol() + (colDistance / 2);
		if (preyRow >= 0 && preyRow < ROW && preyCol >= 0 && preyCol < COL) {
			Tiles preyTile = theBoard[preyRow][preyCol];
			if (!theBoard[destRow][destCol].isOccupied()
					&& preyTile.isOccupied()
					&& preyTile.getPiece().getSide() != jumper.getSide()) {
				System.out.println("Valid jump: " + jumper.getRow() + "," + jumper.getCol()
						+ " -> prey " + preyRow + "," + preyCol
						+ " -> land " + destRow + "," + destCol);
				return true;
			}
		}
		System.err.println("Cannot jump to " + destRow + "," + destCol);
		return false;
	}

	/**
	 * Checks if a piece has any available jump moves. This method evaluates the potential jump moves for a given piece based on its type (Red, Black, or King) and its current position on the board. It checks all possible jump directions (forward and backward for Kings, forward only for normal pieces) and verifies if the destination tile is unoccupied and if there is an opponent's piece in the correct position to be jumped over. If any valid jump move is found, it returns true; otherwise, it returns false.
	 * @param jumper the piece for which to check jump availability
	 * @return true if there is at least one valid jump move available for the piece, false otherwise
	 */
	public boolean jumpAvailable(Pieces jumper) {
		int row = jumper.getRow();
		int col = jumper.getCol();
		PieceType type = jumper.getType();

		// Directions to check: {rowDelta, colDelta} toward prey
		int[][] directions;
		if (type == PieceType.RED) {
			directions = new int[][]{{1, -1}, {1, 1}};           // south only
		} else if (type == PieceType.BLACK) {
			directions = new int[][]{{-1, -1}, {-1, 1}};         // north only
		} else {
			directions = new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // all 4
		}

		for (int[] dir : directions) {
			int preyR = row + dir[0];
			int preyC = col + dir[1];
			int landR = row + dir[0] * 2;
			int landC = col + dir[1] * 2;
			if (landR >= 0 && landR < ROW && landC >= 0 && landC < COL) {
				Tiles preyTile = theBoard[preyR][preyC];
				if (preyTile.isOccupied()
						&& preyTile.getPiece().getSide() != jumper.getSide()
						&& !theBoard[landR][landC].isOccupied()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Executes the jump described by the current destRow/destCol and preyRow/preyCol.
	 * checkJump(jumper) MUST be called before this to validate and set those fields.
	 */
	public void jumpPieces(Pieces jumper) {
		// Use jumper's own stored position as the source (not the shared currentRow/currentCol)
		// to avoid any desync from the shared mutable fields.
		int srcRow = jumper.getRow();
		int srcCol = jumper.getCol();
		Pieces prey = theBoard[preyRow][preyCol].getPiece();
		if (prey == null) {
			System.err.println("No piece to jump over at " + preyRow + "," + preyCol);
			return;
		}
		theBoard[destRow][destCol].addPiece(jumper);
		theBoard[srcRow][srcCol].delete();
		jumper.moved(destRow, destCol);
		lastPieceMoved = jumper;
		checkingTheCrown(jumper, destRow, destCol);
		theBoard[preyRow][preyCol].delete();
		if (prey.getSide() == PlayerType.RED) {
			RED.pieceEaten();
		} else {
			BLACK.pieceEaten();
		}
		checkWin();
		// Keep currentRow/currentCol pointing at the piece's new position for multi-jump
		currentRow = destRow;
		currentCol = destCol;
	}

	/**
	 * Moves a piece to the specified destination coordinates. This method is called when a piece is selected and a valid destination tile is clicked. 
	 * It checks if the move is valid based on the type of the piece (normal or King) and the rules of movement for that piece. If the move is valid, it updates the piece's position on the board, checks for crowning if necessary, and switches turns. If the move is invalid, it prints an error message and does not update the game state.
	 * @param dRow the destination row for the piece
	 * @param dCol the destination column for the piece
	 */
	public void movePieces(int dRow, int dCol) {
		destRow = dRow;
		destCol = dCol;
		System.out.println(currentRow + "," + currentCol
				+ " would like to go to " + destRow + "," + destCol);
		if (isComputerTurn()) {
			System.err.println("Wait for the computer to finish its turn");
			return;
		}
		if (!theBoard[currentRow][currentCol].isOccupied()) {
			System.err.println("No piece selected - please click your piece first");
			return;
		}

		Pieces selected = theBoard[currentRow][currentCol].getPiece();
		if (selected.getSide() != turn()) {
			System.err.println("No piece selected - please click your piece first");
			return;
		}
		if (continuousJump && (currentRow != lastPieceMoved.getRow()
				|| currentCol != lastPieceMoved.getCol())) {
			System.err.println("You must continue capturing with the same piece");
			return;
		}

		CheckersAI.Move guiMove = CheckersAI.findLegalStep(theBoard, turn(),
				currentRow, currentCol, destRow, destCol);
		if (guiMove == null) {
			System.err.println("Invalid move. If a capture is available, it must be taken.");
			return;
		}

		applyBoardMove(guiMove);
		if (guiMove.isCapture() && CheckersAI.hasCaptureFrom(theBoard, turn(),
				guiMove.getToRow(), guiMove.getToCol())) {
			continuousJump = true;
			currentRow = guiMove.getToRow();
			currentCol = guiMove.getToCol();
			panel.repaint();
			System.out.println("Another capture is available with the same piece.");
			return;
		}

		continuousJump = false;
		if (frame.isDisplayable()) {
			switchTurns();
			panel.repaint();
			maybeStartComputerTurn();
		}
	}

	public void movePiecesOld(int dRow, int dCol) {
		int startingTurn = turnCounter;
		destRow = dRow;
		destCol = dCol;
		System.out.println(currentRow + "," + currentCol
				+ " would like to go to " + destRow + "," + destCol);
		if ((theBoard[currentRow][currentCol].isOccupied())
				&& ((destRow + destCol) % 2 == 1)) { // Gray tiles only
			Pieces root = theBoard[currentRow][currentCol].getPiece();

			// Guard: currentRow/currentCol can be stale from a previous turn
			// if the player clicks a destination before selecting their own piece.
			if (root.getSide() != turn()) {
				System.err.println("No piece selected — please click your piece first");
				return;
			}

			// Use the piece's own stored position as ground truth for all
			// calculations, avoiding any desync with the shared currentRow/currentCol.
			int pieceRow = root.getRow();
			int pieceCol = root.getCol();

			if (!jumpAvailable(root)) {
				// ── Normal move ──────────────────────────────────────────────
				boolean diagonal1Step =
						(Math.abs(destRow - pieceRow) == 1)
						&& (Math.abs(destCol - pieceCol) == 1);

				if (root.getType() == PieceType.BLACK_KING
						|| root.getType() == PieceType.RED_KING) {
					if (diagonal1Step && !theBoard[destRow][destCol].isOccupied()) {
						theBoard[destRow][destCol].addPiece(root);
						theBoard[pieceRow][pieceCol].delete();
						root.moved(destRow, destCol);
						lastPieceMoved = root;
						System.out.println("King moved to " + destRow + "," + destCol);
						switchTurns();
					}

				} else if (root.getType() == PieceType.RED
						|| root.getType() == PieceType.BLACK) {

					boolean correctDirection =
							(root.getType() == PieceType.RED  && destRow > pieceRow)
							|| (root.getType() == PieceType.BLACK && destRow < pieceRow);

					if (!correctDirection) {
						System.err.println("Normal pieces can't move backwards");
						return;
					}
					if (diagonal1Step && !theBoard[destRow][destCol].isOccupied()) {
						theBoard[destRow][destCol].addPiece(root);
						theBoard[pieceRow][pieceCol].delete();
						root.moved(destRow, destCol);
						lastPieceMoved = root;
						switchTurns();
						System.out.println("Piece moved to " + destRow + "," + destCol);
						checkingTheCrown(root, destRow, destCol);
					}
				}

			} else {
				// ── Jump required ────────────────────────────────────────────
				if (checkJump(root)) {
					jumpPieces(root);
					if (jumpAvailable(lastPieceMoved)) {
						continuousJump = true;
						currentRow = lastPieceMoved.getRow();
						currentCol = lastPieceMoved.getCol();
					} else {
						continuousJump = false;
						switchTurns();
					}
				} else {
					System.err.println("A jump is available with the selected piece"
							+ " — destination must be exactly 2 diagonal steps away"
							+ " over an enemy piece.");
				}
			}
		} else {
			System.err.println("Cannot move to that tile");
			return;
		}
		if (turnCounter != startingTurn) {
			maybeStartComputerTurn();
		}
	}

	/**
	 * Displays a dialog box announcing the losing player. This method is called when a win condition is met (i.e., one player has no pieces left), and it shows a message indicating which player lost the game. After the dialog is displayed, the game frame is closed.
	 */
	public void displayDialog() {
		JOptionPane.showMessageDialog(frame, loser
				+ "-side player lost! Well played!");
	}

	/**
	 * Creates the menu bar for the game, including the "File" menu with "Help" and "Resign" options. The "Help" option displays a dialog with instructions on how to play the game, while the "Resign" option allows the current player to concede the game, declaring the other player as the winner. The menu is added to the game frame for user interaction.
	 */
	public void createMenu() {
		menuBar = new JMenuBar();
		menu = new JMenu("File");
		resign = new JMenuItem("Resign");
		help = new JMenuItem("Help");
		help.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane
						.showMessageDialog(
								frame,
								"Checkers/Draughts is a board game designed to be played by two players.\n"
										+ "\nThe objective is to eat all the pieces of the other side. This game is played only on the darker tiles of the board."
										+ "\nNormal pieces may only move diagonally forward one space at a time, if a same-side piece is present, they are not able to move."
										+ "\nPieces may only eat other-side pieces if there is another piece diagonal to them, and the tile behind that piece is open."
										+ "\nIf the opportunity to eat a piece is present, the player must eat the piece.\n"
										+ "\nNormal pieces that reach the other end of the board from their side are crowned king. Kings may move diagonally forwards and backwards.\n"
										+ "\nThe first move is made by the black player side. Good luck and have fun!");
			}

		});
		resign.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (turnCounter % 2 == 1) {
					RED.lost();
					loser = "Red";
				} else {
					BLACK.lost();
					loser = "Black";
				}
				JOptionPane.showMessageDialog(frame, loser
						+ "-side player resigned! Good game.");
				frame.dispose();
			}
		});
		menuBar.add(menu);
		menu.add(help);
		menu.add(resign);
		frame.setJMenuBar(menuBar);
	}

	public static void main(String[] args) {
		Object[] modes = {"GUI", "Text-based (Console)"};
		int choice = JOptionPane.showOptionDialog(
				null,
				"How would you like to play?",
				"Checkers - Game Mode",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				modes,
				modes[0]);
		if (choice == 1) {
			new TextGame();
		} else {
			new Board(); // GUI mode — Board's constructor shows the board-size dialog
		}
	}
}
