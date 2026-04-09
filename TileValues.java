package Checkers;

public class TileValues extends Tiles {
    private int value;
    public TileValues(int row, int col, Board b, int value) {
        super(row, col, b);
        this.setValue(value);
        // TODO Auto-generated constructor stub
    }

    public void giveValue(int val) {
            if (this.getValue() == 0) {
                this.setValue(val);
            }

    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
    
    

}
