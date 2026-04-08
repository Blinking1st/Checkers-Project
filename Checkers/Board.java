package Checkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
	private ArrayList<Pieces> redPieces = new ArrayList<>(); // Holds all the
	// pieces.
	private ArrayList<Pieces> blackPieces = new ArrayList<>(); // Used in
	// construction
	// of the board
	private JMenuBar menuBar;
	private JMenu menu; // Menu
	private JMenuItem help;
	private JMenuItem resign; // Resign option
	private int redCounter = 0;
	private int blackCounter = 0;
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
	private ArrayList<Pieces> nextPiece = new ArrayList<>();
	private String loser; // Prints losing side

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
		frame.add(panel);
		frame.setVisible(true);
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

	/**
	 * Switches turns after a move is made. Increments the turn counter, which is used to determine which player's turn it is.
	 */
	public void switchTurns() { // Once a move is made, switch/increment turns
		turnCounter++;
	}

	/**
	 * Clears the list of potential moves for a piece. This is used to reset the state of the game after a move is made, ensuring that the next piece's potential moves are calculated correctly.
	 */
	public void clearPotentialMoves() {
		nextPiece.clear();
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
						redPieces.add(new Pieces(i, j, PieceType.RED));
						theBoard[i][j].addPiece(redPieces.get(redCounter));
						redCounter++;
					} else if (i >= ROW - pieceRows) {
						blackPieces.add(new Pieces(i, j, PieceType.BLACK));
						theBoard[i][j].addPiece(blackPieces.get(blackCounter));
						blackCounter++;
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

	/**
	 * Checks if a piece can perform a jump move. This method calculates the potential jump move based on the current position of the piece and the destination coordinates. It checks if the destination tile is unoccupied and if there is an opponent's piece (the "prey") in the correct position to be jumped over. If both conditions are met, it returns true, indicating that the jump is valid; otherwise, it returns false and switches turns.
	 * @param jumper the piece that is attempting to jump
	 * @return true if the jump is valid, false otherwise
	 */
	public boolean checkJump(Pieces jumper) { // Checks if a piece may jump.
		// Jumper is the clicked piece.
		// For very direct movements
		currentRow = jumper.getRow();
		currentCol = jumper.getCol();
		System.out.println("Jumper begins at " + currentRow + "," + currentCol);
		int rowDistance = (destRow - jumper.getRow());
		int colDistance = (destCol - jumper.getCol());
		preyRow = jumper.getRow() + (rowDistance / 2); // Location of "prey"
		// piece
		preyCol = jumper.getCol() + (colDistance / 2);
      if (preyRow >= 0 && preyRow <= ROW - 1 && preyCol >= 0 && preyCol <= COL - 1) {
		if (!theBoard[destRow][destCol].isOccupied() // If the destination is
				// not occupied and the
				// "prey" location is
				&& (theBoard[preyRow][preyCol].isOccupied())) {
			return true;
		} else {
			System.err.println("Cannot jump to " + destRow + "," + destCol);
			switchTurns();
			return false;
			}
		}
		return false;
	}

	/**
	 * Checks if a piece has any available jump moves. This method evaluates the potential jump moves for a given piece based on its type (Red, Black, or King) and its current position on the board. It checks all possible jump directions (forward and backward for Kings, forward only for normal pieces) and verifies if the destination tile is unoccupied and if there is an opponent's piece in the correct position to be jumped over. If any valid jump move is found, it returns true; otherwise, it returns false.
	 * @param jumper the piece for which to check jump availability
	 * @return true if there is at least one valid jump move available for the piece, false otherwise
	 */
	public boolean jumpAvailable(Pieces jumper) { // Checks numerous potential
		// destinations
		int switchCase = 0, RowMovement = 0, jumperRow = 0, jumperCol = 0;
		if (jumper.getType() == PieceType.RED) { // Depending on the piece type,
			// switch case checks
			// different areas
			switchCase = 1;
			RowMovement = 2; // Red pieces may only move South
		} else if (jumper.getType() == PieceType.BLACK) {
			switchCase = 2; // Black pieces may only move North
			RowMovement = -2;
		} else if (jumper.getType() == PieceType.RED_KING // Kings move in all 4
				// directions
				|| jumper.getType() == PieceType.BLACK_KING) {
			switchCase = 3;
		}

		jumperRow = jumper.getRow();
		jumperCol = jumper.getCol();

		switch (switchCase) {
			case 1: { // Red pieces
				if ((jumperRow > -1 && jumperRow < ROW)
						&& (jumperCol > -1 && jumperCol < COL)) { // Checks if within
					// board bounds
					if ((jumperRow + RowMovement) <= ROW - 1) {
						if (jumperCol != COL - 1 && jumperCol < COL - 2 && jumperCol != 0
								&& jumperCol > 1) { // If the selected piece is not
							// near any edges
							if (!theBoard[jumperRow + RowMovement][jumperCol + 2] // Check
									// right
									// location
									.isOccupied()
									&& theBoard[jumperRow + 1][jumperCol + 1]
									.isOccupied()) {
								return true;
							}
							if (!theBoard[jumperRow + RowMovement][jumperCol - 2] // Check
									// left
									// location
									.isOccupied()
									&& theBoard[jumperRow + 1][jumperCol - 1]
									.isOccupied()) {
								return true;
							}
							return false;
						}

						if (jumperCol >= COL - 2) { // if jumper is close to right edge
							if (!theBoard[jumperRow + RowMovement][jumperCol - 2]
									.isOccupied()
									&& theBoard[jumperRow + 1][jumperCol - 1]
									.isOccupied()) {
								return true;
							}
						}
						if (jumperCol <= 1) {
							if (!theBoard[jumperRow + RowMovement][jumperCol + 2]
									.isOccupied()
									&& theBoard[jumperRow + 1][jumperCol + 1]
									.isOccupied()) {
								return true;
							}
						}
						return false;
					} else
						return false;
				}
			}
			break;
			case 2: { // Black pieces
				if ((jumperRow > -1 && jumperRow < ROW)
						&& (jumperCol > -1 && jumperCol < COL)) { // Checks if within
					// board bounds
					if ((jumperRow + RowMovement) > -1) { // if row within bounds

						if (jumperCol != COL - 1 && jumperCol < COL - 2 && jumperCol != 0
								&& jumperCol > 1) { // if column not near edges

							if (!theBoard[jumperRow + RowMovement][jumperCol + 2]
									.isOccupied()
									&& theBoard[jumperRow - 1][jumperCol + 1]
									.isOccupied()) {
								return true;
							}
							if (!theBoard[jumperRow + RowMovement][jumperCol - 2]
									.isOccupied()
									&& theBoard[jumperRow - 1][jumperCol - 1]
									.isOccupied()) {
								return true;
							}
							return false;
						}
						if (jumperCol >= COL - 2) { // if jumper is close to right edge
							if (!theBoard[jumperRow + RowMovement][jumperCol - 2]
									.isOccupied()
									&& theBoard[jumperRow - 1][jumperCol - 1]
									.isOccupied()) {
								return true;
							}
						}
						if (jumperCol <= 1) {
							if (!theBoard[jumperRow + RowMovement][jumperCol + 2]
									.isOccupied()
									&& theBoard[jumperRow - 1][jumperCol + 1]
									.isOccupied()) {
								return true;
							}
						}
						return false;
					} else
						return false;
				}
			}
			break;
			case 3: { // King availability
				int KingNorth = jumperRow - 2;
				int KingEast = jumperCol + 2;
				int KingSouth = jumperRow + 2;
				int KingWest = jumperCol - 2;
				if (KingSouth <= ROW - 1 && KingEast <= COL - 1 && KingNorth >= 0
						&& KingWest >= 0) { // If destination is within bounds
					System.out.println(KingSouth + " " + KingEast + " " + KingNorth
							+ " " + KingWest);
					if (!theBoard[KingNorth][KingEast].isOccupied()
							&& theBoard[jumperRow - 1][jumperCol + 1].isOccupied()) {
						System.out.println("NorthEast open");
						return true;
					}
					if (!theBoard[KingNorth][KingWest].isOccupied()
							&& theBoard[jumperRow - 1][jumperCol - 1].isOccupied()) {
						System.out.println("NorthWest open");
						return true;
					}
					if (!theBoard[KingSouth][KingEast].isOccupied()
							&& theBoard[jumperRow + 1][jumperCol + 1].isOccupied()) {
						System.out.println("SouthEast open");
						return true;
					}
					if (!theBoard[KingSouth][KingWest].isOccupied()
							&& theBoard[jumperRow + 1][jumperCol - 1].isOccupied()) {
						System.out.println("SouthWest open");
						return true;
					}
				}
				if ((jumperRow == 0 || jumperRow == 1)
						&& ((KingEast <= COL - 1) && (KingWest >= 0))) { // near north edge
					if (!theBoard[KingSouth][KingEast].isOccupied()
							&& theBoard[jumperRow + 1][jumperCol + 1].isOccupied()) {
						return true;
					}
					if (!theBoard[KingSouth][KingWest].isOccupied()
							&& theBoard[jumperRow + 1][jumperCol - 1].isOccupied()) {
						return true;
					}
				}
				if ((jumperRow == ROW - 1 || jumperRow == ROW - 2)
						&& ((KingEast <= COL - 3) && (KingWest >= 0))) { // near south edge
					if (!theBoard[KingNorth][KingEast].isOccupied()
							&& theBoard[jumperRow - 1][jumperCol + 1].isOccupied()) {
						return true;
					}
					if (!theBoard[KingNorth][KingWest].isOccupied()
							&& theBoard[jumperRow - 1][jumperCol - 1].isOccupied()) {
						return true;
					}
				}
				if ((jumperRow == ROW - 1 || jumperRow == ROW - 2)
						&& ((KingNorth >= 0) && (KingSouth <= ROW - 3))) { // near right edge
					if (!theBoard[KingNorth][KingWest].isOccupied()
							&& theBoard[jumperRow - 1][jumperCol - 1].isOccupied()) {
						return true;
					}
					if (!theBoard[KingSouth][KingWest].isOccupied()
							&& theBoard[jumperRow + 1][jumperCol - 1].isOccupied()) {
						return true;
					}
				}
				if ((jumperRow == 0 || jumperRow == 1)
						&& ((KingNorth >= 0) && (KingSouth <= ROW - 3))) { // near left edge
					if (!theBoard[KingNorth][KingEast].isOccupied()
							&& theBoard[jumperRow - 1][jumperCol + 1].isOccupied()) {
						return true;
					}
					if (!theBoard[KingSouth][KingEast].isOccupied()
							&& theBoard[jumperRow + 1][jumperCol + 1].isOccupied()) {
						return true;
					}
				}
				return false;
			}
			default:
				System.err.println("Default case");
				break;
		}
		return false;
	}

	/**
	 * Executes a jump move for a piece. This method is called when a piece is selected to perform a jump move. It checks if the jump is valid using the checkJump method, and if it is, it moves the piece to the destination tile, removes the jumped piece from the board, and updates the game state accordingly (e.g., updating the player's piece count, checking for crowning, and checking for win conditions). If there are additional jump moves available for the same piece after the jump, it allows the player to continue jumping with that piece.
	 * @param jumper the piece that is performing the jump
	 */
	public void jumpPieces(Pieces jumper) {

		Pieces prey = theBoard[preyRow][preyCol].getPiece();
		Tiles t = theBoard[currentRow][currentCol];
		jumper = t.getPiece();
		if (checkJump(jumper)) {
			if (jumper.getType() == prey.getType()) {
				jumper.talk();
				prey.talk();
				System.err.println("Cannot eat same side piece");
				return;
			}
			theBoard[destRow][destCol].addPiece(jumper);
			theBoard[currentRow][currentCol].delete();
			jumper.moved(destRow, destCol);
			lastPieceMoved = jumper;
			checkingTheCrown(jumper, destRow, destCol);
			theBoard[preyRow][preyCol].delete();
			if (prey.getType() == PieceType.RED
					|| prey.getType() == PieceType.RED_KING) {
				RED.pieceEaten();
			} else if (prey.getType() == PieceType.BLACK
					|| prey.getType() == PieceType.BLACK_KING) {
				BLACK.pieceEaten();
			}
			checkWin();
		}
		if (jumpAvailable(lastPieceMoved)) {
			if (checkJump(lastPieceMoved)) {
				jumpPieces(lastPieceMoved);
			} else
				return;
		} else
			return;
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
		if ((theBoard[currentRow][currentCol].isOccupied())
				&& ((destRow + destCol) % 2 == 1)) { // Gray tiles
			Pieces root = theBoard[currentRow][currentCol].getPiece();
			if (jumpAvailable(root) == false) {
				if (root.getType() == PieceType.BLACK_KING
						|| root.getType() == PieceType.RED_KING) {
					if ((Math.abs(destRow - currentRow) == 1)
							|| (Math.abs(destCol - currentCol) == 1)) {

						if (theBoard[destRow][destCol].isOccupied() == false) {
							theBoard[destRow][destCol].addPiece(root);
							theBoard[currentRow][currentCol].delete();
							root.moved(destRow, destCol);
							lastPieceMoved = root;
							System.out.println("Root piece moved to " + destRow
									+ "," + destCol);
							switchTurns();
						}
					}

					// Normal piece movement
				} else if ((root.getType() == PieceType.BLACK || root.getType() == PieceType.RED)) {

					if ((root.getType() == PieceType.RED && (destRow > currentRow))
							|| (root.getType() == PieceType.BLACK && (destRow < currentRow))) {
						if ((Math.abs(destRow - currentRow) == 1)
								|| (Math.abs(destCol - currentCol) == 1)) {

							if (theBoard[destRow][destCol].isOccupied() == false) {
								theBoard[destRow][destCol].addPiece(root);
								theBoard[currentRow][currentCol].delete();
								root.moved(destRow, destCol);
								lastPieceMoved = root;
								// System.out.println("Last piece moved "
								// + lastPieceMoved.getRow() + ","
								// + lastPieceMoved.getCol());
								switchTurns();
								System.out.println("Root piece moved to "
										+ destRow + "," + destCol);
								checkingTheCrown(root, destRow, destCol);
							}
						}

					} else {
						System.err
								.println("Normal pieces can't move backwards");
						return;
					}
				}
			} else {
				if (checkJump(root)) {
					jumpPieces(root);
					switchTurns();
				}
			}
		} else {
			System.err.println("Cannot move onto white tile bounds");
			return;
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
										+ "\nThe objective is to \"eat\" all the pieces of the other side. This game is played only on the darker tiles of the board."
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