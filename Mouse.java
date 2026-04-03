package Checkers;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

public class Mouse implements MouseListener {
	final int Range = 75;
	private int selectedRow;
	private int selectedCol;
	private Tiles tile;
	private Pieces piece;
	private Board board;

	@Override
	/**
	 * Handles mouse press events on the game board. When a tile is clicked, it checks if the tile is occupied by a piece and if the piece belongs to the current player. If so, it selects the piece and highlights the tile. If the tile is not occupied, it processes the click as a potential move or jump for the currently selected piece. The method also updates the game state based on the player's actions and provides feedback through console output for debugging purposes.
	 * @param e the MouseEvent triggered by the mouse press
	 */
	public void mousePressed(MouseEvent e) {		//The Mouse Listener handles most of the events that occur in this program
												
		tile = (Tiles) e.getSource();	
		tile.setBorder((BorderFactory.createLoweredBevelBorder()));
		if (tile == null) {
			return; // Should never happen
		}
		this.board = tile.getBoard();
		if (tile.isOccupied()) { //Determines if the tile holds a piece
			this.piece = tile.getPiece(); // Gets the piece at the tile.

			if (piece.getSide() == board.turn()) { //If the turn corresponds with Red/Black player's turn
				System.out.println("---------------------");	//Output for organized debugging
				board.clearPotentialMoves();
				this.selectedRow = this.piece.getRow(); // Get selected piece coordinates
				this.selectedCol = this.piece.getCol();
				tile.selected(true);					//Passed as selected!	
				this.board.getRootRowCol(this.selectedRow, this.selectedCol);	//Gives the board the coordinates of the clicked tile/piece
				System.out.println("Piece occupies: " + selectedRow + ","
						+ selectedCol);
			} else {
				System.err.println("It's not your turn");
				return;
			}
		} else if (!tile.isOccupied()) {		//If tile has no pieces within it...
			tile.coord();						
			tile.selected(false);				//Head to board functions
		}
	}

	@Override
	/**
	 * Handles mouse click events on the game board. This method is currently empty, as the primary logic for handling piece selection and movement is implemented in the mousePressed method. However, it can be used in the future to implement additional functionality that should occur when a tile is clicked, such as providing visual feedback or triggering specific game actions.
	 * @param e the MouseEvent triggered by the mouse click
	 */
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	/**
	 * Handles mouse release events on the game board. When the mouse button is released after clicking on a tile, this method resets the border of the tile to its default state (removing any highlighting). This provides visual feedback to the player that the tile is no longer selected or active after the click action is completed.
	 * @param e the MouseEvent triggered by the mouse release
	 */
	public void mouseReleased(MouseEvent e) {
	tile.setBorder(BorderFactory.createEmptyBorder());
	}

	@Override
	/**
	 * Handles mouse enter events on the game board. This method is currently empty, but it can be used to implement functionality that should occur when the mouse cursor enters a tile, such as highlighting the tile or displaying additional information about the tile or piece. This can enhance the user experience by providing visual cues or feedback when hovering over different parts of the game board.
	 * @param e the MouseEvent triggered by the mouse entering a tile
	 */
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	/**
	 * Handles mouse exit events on the game board. This method is currently empty, but it can be used to implement functionality that should occur when the mouse cursor exits a tile, such as removing any highlighting or visual cues that were applied when the mouse entered the tile. This can help maintain a clean and intuitive user interface by ensuring that visual feedback is appropriately updated as the player interacts with different tiles on the game board.
	 * @param e the MouseEvent triggered by the mouse exiting a tile
	 */
	public void mouseExited(MouseEvent e) {
	}
}
