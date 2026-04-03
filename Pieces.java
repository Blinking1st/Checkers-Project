package Checkers;
import javax.swing.JComponent;

public class Pieces extends JComponent {

	private PieceType type;
	private PlayerType side;
	private int thisRow = 0;
	private int thisCol = 0;
	private boolean potentialLocation;
	private boolean clicked = false;

	/**
	 * Constructor for the Pieces class. Initializes a piece with its row and column coordinates, as well as its type (Red, Red King, Black, or Black King). The constructor also determines the side (Red or Black) based on the type of the piece. This setup allows the piece to be properly represented on the game board and interact according to the rules of Checkers.
	 * @param row The row coordinate of the piece
	 * @param column The column coordinate of the piece
	 * @param type The type of the piece (Red, Red King, Black, or Black King)
	 */
	public Pieces(int row, int column, PieceType type) {
		if (type == PieceType.RED || type == PieceType.RED_KING) {
			side = PlayerType.RED;
		} else if (type == PieceType.BLACK || type == PieceType.BLACK_KING) {
			side = PlayerType.BLACK;
		}
		thisRow = row;
		thisCol = column;
		this.type = type;
	}

	/**
	 * Returns the type of the piece (Red, Red King, Black, or Black King). This method allows other parts of the program to identify the specific type of piece, which is essential for determining valid moves, jumps, and interactions with other pieces on the game board. The type information is crucial for enforcing the rules of Checkers and ensuring that each piece behaves according to its designated role in the game.
	 * @return the type of the piece (Red, Red King, Black, or Black King)
	 */
	public PieceType getType() {
		return this.type;
	}

	/**
	 * Crowns the piece if it reaches the opposite end of the board. This method checks the current type of the piece and updates it to the corresponding King type if it is a normal piece (Red or Black). Crowning a piece allows it to move both forward and backward, which is a key aspect of Checkers strategy. The method also prints a message to the console indicating that the piece has been crowned, which can be useful for debugging and tracking game events.
	 */
	public void crowned() {
		if (type == PieceType.RED) {
			type = PieceType.RED_KING;
			System.out.println("Red crowned");
		} else if (type == PieceType.BLACK) {
			type = PieceType.BLACK_KING;
			System.out.println("Black crowned");
		}
	}

	/**
	 * Returns the side (Red or Black) that the piece belongs to. This method allows other parts of the program to determine which player controls the piece, which is essential for enforcing turn-based gameplay and validating moves. Knowing the side of a piece is crucial for implementing game logic that ensures players can only move their own pieces and interact with opponent pieces according to the rules of Checkers.
	 * @return the side (Red or Black) that the piece belongs to
	 */
	public PlayerType getSide() {
		return this.side;
	}

	/**
	 * Returns the current row coordinate of the piece. This method allows other parts of the program to track the position of the piece on the game board, which is essential for validating moves, jumps, and interactions with other pieces. The row coordinate is a key component of the piece's location and is used in conjunction with the column coordinate to determine valid moves and game state.
	 * @return the current row coordinate of the piece
	 */
	public int getRow() {
		return this.thisRow;
	}

	/**
	 * Returns the current column coordinate of the piece. This method allows other parts of the program to track the position of the piece on the game board, which is essential for validating moves, jumps, and interactions with other pieces. The column coordinate is a key component of the piece's location and is used in conjunction with the row coordinate to determine valid moves and game state.
	 * @return the current column coordinate of the piece
	 */
	public int getCol() {
		return this.thisCol;
	}
	
	/**
	 *  Prints the current position of the piece to the console. This method is useful for debugging purposes, allowing developers to track the location of pieces on the game board as they are moved or interacted with. By calling this method, you can easily see the row and column coordinates of a piece, which can help identify issues with movement logic or game state updates.	
	 */
	public void talk(){
		System.out.println("Piece is at " + thisRow + "," + thisCol);
	}

	/**
	 * Updates the piece's position to the specified row and column coordinates. This method is called when a piece is moved on the game board, allowing it to update its internal state to reflect its new location. By providing the new row and column, this method ensures that the piece's position is accurately maintained throughout the game, which is essential for validating moves, jumps, and interactions with other pieces according to the rules of Checkers.
	 * @param row the new row coordinate for the piece
	 * @param col the new column coordinate for the piece
	 */
	public void moved(int row, int col) { // Changes coordinates after being
											// moved
		System.out.println("Moved to " + row + "," + col);
		thisRow = row;
		thisCol = col;
	}

	/** 
	 * Marks the piece as a potential move location. This method is called when a tile is clicked that is not occupied by a piece, indicating that it may be a valid destination for a move or jump. By setting the potentialLocation flag to true, this method allows the game logic to recognize that the piece can potentially move to this location, which is essential for validating moves and providing feedback to the player about possible actions on the game board.
	 * @return true if the location is marked as a potential move, false otherwise
	*/
	public boolean potentialMove() {
		potentialLocation = true;
		return potentialLocation;
	}
}
