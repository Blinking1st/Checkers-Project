package Checkers;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
/**
 * Class representing a tile on the Checkers game board. Each tile has specific coordinates (row and column) and can hold a piece. The Tiles class provides methods for adding and removing pieces, checking if the tile is occupied, and handling selection for potential moves. The paintComponent method is overridden to visually represent the tile and any piece it may contain, allowing for a graphical representation of the game board. This class is essential for managing the state of each tile and facilitating interactions between pieces and the game board.
 */
public class Tiles extends JPanel {
	final private int WIDTH = 75;
	final private int HEIGHT = 75;
	private Pieces piece;
	private Board board;
	private boolean PieceAdded = false;
	private int row;
	private int col;
	private boolean select;

	/**
	 * Constructor for the Tiles class. Initializes a tile with specific row and column coordinates and associates it with the game board. The constructor sets up the tile's position on the board and prepares it to hold pieces as the game progresses. By providing the row, column, and board reference, this constructor allows the tile to interact with the overall game state and respond to player actions effectively.
	 * @param row the row coordinate of the tile on the game board
	 * @param col the column coordinate of the tile on the game board
	 * @param b the reference to the game board that the tile belongs to
	 */
	public Tiles(int row, int col, Board b) {
		this.row = row;
		this.col = col;
		this.board = b;
	}

	/**
	 * Returns the reference to the game board that the tile belongs to. This method allows other parts of the program to access the board associated with this tile, which is essential for managing game state and facilitating interactions between tiles and pieces. By calling this method, you can retrieve the board reference needed for various operations, such as validating moves, updating the game state, or accessing other tiles and pieces on the board.
	 * @return the reference to the game board that the tile belongs to
	 */
	public Board getBoard() {
		return this.board;
	}

	/**
	 * Prints the coordinates of the tile to the console. This method is typically called when a tile is clicked, providing feedback about which tile has been selected. By outputting the row and column coordinates, this method can assist with debugging and tracking player interactions with the game board, allowing developers to verify that the correct tiles are being selected and processed during gameplay.
	 */
	public void coord() {
		System.out.println("This is tile (" + row + "," + col + ").");
	}

	/**
	 * Returns the current row coordinate of the tile. This method allows other parts of the program to access the tile's row position on the game board, which is essential for validating moves, jumps, and interactions with pieces. The row coordinate is a key component of the tile's location and is used in conjunction with the column coordinate to determine valid moves and game state.
	 * @return the current row coordinate of the tile
	 */
	public int getRow() {
		return row;
	}

	/**
	 * Returns the current column coordinate of the tile. This method allows other parts of the program to access the tile's column position on the game board, which is essential for validating moves, jumps, and interactions with pieces. The column coordinate is a key component of the tile's location and is used in conjunction with the row coordinate to determine valid moves and game state.
	 * @return the current column coordinate of the tile
	 */
	public int getCol() {
		return col;
	}

	/**
	 * Adds a piece to the tile. This method is called when a piece is moved to this tile or when a new piece is placed on the board. By providing the piece to be added, this method updates the tile's state to reflect that it is now occupied by a piece, allowing for proper management of game state and interactions between pieces and tiles. The PieceAdded flag is set to true to indicate that the tile now contains a piece.
	 * @param p the piece to be added to the tile
	 */
	public void addPiece(Pieces p) {
		piece = p;
		PieceAdded = true;
	}

	/**
	 * Checks if the tile is currently occupied by a piece. This method returns true if there is a piece on the tile and false otherwise. By calling this method, other parts of the program can determine whether a tile is available for movement or if it is already occupied, which is essential for validating moves and enforcing game rules related to piece placement and interactions on the game board.
	 * @return true if the tile is occupied by a piece, false otherwise
	 */
	public boolean isOccupied() {
		if (piece != null) {
			return true;
		} else
			return false;
	}

	/**
	 * Handles the selection of the tile for potential moves. When a tile is selected, it checks if the selection is valid and updates the game state accordingly. If the tile is not occupied, it processes the selection as a potential move destination for the currently selected piece. By calling this method with a boolean value, you can manage the selection state of the tile and trigger appropriate actions based on player interactions with the game board.
	 * @param bool a boolean value indicating whether the tile is being selected (true) or deselected (false)
	 */
	public void selected(boolean bool) {
		this.select = bool;
		if (this.select == false) { // If the destination is a non-piece holding tile, it will set the destination coordinates
			board.movePieces(row, col); // Gives board destination coordinates
		}
		this.select = false;		//Refreshes the function
	}

	/**
	 * Returns the piece currently occupying the tile. This method allows other parts of the program to access the piece on the tile, which is essential for validating moves, jumps, and interactions between pieces and tiles. By calling this method, you can retrieve the piece that is currently on the tile, which can help determine valid moves and game state based on the piece's type and ownership.
	 * @return the piece currently occupying the tile, or null if the tile is not occupied
	 */
	public Pieces getPiece() {
		return this.piece;
	}

	/**
	 * Removes the piece from the tile. This method is called when a piece is captured (eaten) or moved away from the tile, allowing the tile to update its state to reflect that it is no longer occupied by a piece. By calling this method, you can effectively clear the tile of any piece, which is essential for managing game state and enforcing rules related to piece captures and movements on the game board.
	 */
	public void delete() {
		this.piece = null;
	}

	/**
	 * Overrides the paintComponent method to visually represent the tile and any piece it may contain. This method paints the tile with alternating colors (white and gray) based on its coordinates, and if a piece is present, it draws a circle representing the piece in the appropriate color (black or red). Additionally, it distinguishes between regular pieces and kings by using different colors for each type. The repaint() call ensures that the visual representation is continuously updated to reflect changes in the game state, such as pieces being moved or captured.
	 * @param g the Graphics object used for drawing the tile and pieces
	 */
	protected void paintComponent(Graphics g) { // Paints tiles. If piece is
												// present, paints a circle
												// within the tile.
		Graphics2D g2 = (Graphics2D) g;

		if ((row + col) % 2 == 0) {
			g2.setColor(Color.WHITE);
		} else {
			g2.setColor(Color.GRAY);
		}
		g2.fillRect(0, 0, WIDTH, HEIGHT);

		if (piece != null) {
			if (piece.getType() == PieceType.BLACK
					|| piece.getType() == PieceType.BLACK_KING) {
				g2.setColor(Color.BLACK);
				if (piece.getType() == PieceType.BLACK_KING) {
					g2.setColor(Color.LIGHT_GRAY);					//Black kings become light gray
				}
			} else if (piece.getType() == PieceType.RED
					|| piece.getType() == PieceType.RED_KING) {
				g2.setColor(Color.RED);
				if (piece.getType() == PieceType.RED_KING) {
					g2.setColor(Color.YELLOW);						//Red kings become bright magenta
				}
			}
			g2.fillOval(5, 5, 65, 65); // Creates "checker" look
		}
		repaint();		//Continuously repaint to make sure the pieces appear
	}
}
