package Checkers;

/**
 * Class representing a player in the Checkers game. Each player has a certain number of pieces and belongs to a specific side (Red or Black). The Player class provides methods for tracking the number of pieces left, handling piece captures, and determining when a player has lost the game. This class is essential for managing the state of each player and enforcing the rules of Checkers regarding piece counts and game outcomes.
 */
public class Player {
	private int pieces = 0;
	private PlayerType side;

	/**
	 * Constructor for the Player class. Initializes a player with a specified side (Red or Black) and sets the initial number of pieces to 12, which is the standard starting count for each player in Checkers. The constructor also assigns the player's side, which is essential for determining valid moves and interactions with pieces on the game board throughout the course of the game.
	 * @param type the side (Red or Black) that the player belongs to
	 */
	public Player(PlayerType type) {
		side = type;
		pieces = 20; //changes amount of peices player has
	}

	/**
	 * Returns the number of pieces left for the player. This method allows other parts of the program to track the player's remaining pieces, which is crucial for determining game state and enforcing rules related to piece captures and game outcomes. By calling this method, you can easily check how many pieces a player has left, which can help identify when a player has lost the game or when certain strategic decisions need to be made based on the number of pieces remaining.
	 * @return the number of pieces left for the player
	 */
	public int piecesLeft() {
		return pieces;
	}

	/**
	 * Handles the event of a piece being captured (eaten) by the opponent. This method decrements the player's piece count and checks if the player has lost the game as a result of losing all their pieces. If the player has no pieces left, it calls the lost() method to update the game state accordingly. 
	 * This method is essential for managing the consequences of piece captures and ensuring that the game correctly identifies when a player has been defeated.
	 */
	public void pieceEaten() {
		if(side == PlayerType.RED){
			System.out.println("Red piece eaten");
		}else if (side == PlayerType.BLACK){
			System.out.println("Black piece eaten");
		}
		pieces--;
		if(pieces == 0){
			lost();
		}
	}

	/**
	 * Handles the event of a player losing the game, typically as a result of having no pieces left. This method sets the player's piece count to zero, effectively marking them as defeated in the game. By calling this method, the game can update its state to reflect that the player has lost, which is essential for determining the outcome of the game and declaring a winner.	
	 */
	public void lost() {	//Resignation
		pieces = 0;
	}

}
