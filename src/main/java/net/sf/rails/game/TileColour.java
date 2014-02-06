package net.sf.rails.game;

/**
 * TileColour represents the different colours of Tiles
 */

public enum TileColour {

    RED(-2, false), 
    FIXED(-1, false),
    WHITE(0, true),
    YELLOW(1, true),
    GREEN(2, true),
    BROWN(3, true),
    GREY(4, true)
    ;

    /**
     * The offset to convert tile numbers to tilename index. Colour number 0 and
     * higher are upgradeable.
     */
    
    private final int number;
    private final boolean upgradeable;
    
    private TileColour(int number, boolean upgradeable) {
        this.number = number;
        this.upgradeable = upgradeable;
    };
    
    public int getNumber() {
        return number;
    }
    
    public boolean isUpgradeable() {
        return upgradeable;
    }
    
    public String toText() {
        return this.name().toLowerCase();
    }
    
    public static TileColour valueOfIgnoreCase(String colourName) {
        return TileColour.valueOf(colourName.toUpperCase());
    }
}
