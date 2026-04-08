package Checkers;

/**
 * Represents the state of a single cell on the Checkers board.
 * Each cell tracks whether it is available for play (dark tiles only)
 * and what piece, if any, currently occupies it.
 */
public class Cell {

    /**
     * Describes what is currently occupying a cell.
     * EMPTY means the cell has no piece on it.
     */
    public enum CellContent {
        EMPTY,
        RED,
        BLACK,
        RED_KING,
        BLACK_KING
    }

    private boolean available; // true for dark (playable) tiles, false for light tiles
    private CellContent content;

    /**
     * Creates a cell with the given availability. All cells start empty.
     * @param available true if this is a dark, playable tile
     */
    public Cell(boolean available) {
        this.available = available;
        this.content = CellContent.EMPTY;
    }

    /**
     * Returns whether this cell is a playable (dark) tile.
     * @return true if the cell can hold a piece
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Returns what is currently occupying this cell.
     * @return the CellContent value for this cell
     */
    public CellContent getContent() {
        return content;
    }

    /**
     * Sets the occupant of this cell.
     * @param content the new CellContent value (use EMPTY to clear the cell)
     */
    public void setContent(CellContent content) {
        this.content = content;
    }

    /**
     * Returns whether this cell is occupied by any piece.
     * @return true if a piece of any type is on this cell
     */
    public boolean isOccupied() {
        return content != CellContent.EMPTY;
    }
}
