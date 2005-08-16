/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/MapHex.java,v 1.2 2005/08/16 20:24:19 evos Exp $
 * 
 * Created on 10-Aug-2005
 * Change Log:
 */
package game;

import java.util.regex.*;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class MapHex implements ConfigurableComponentI {
    
    public static final int EW = 0;
    public static final int NS = 1;
    protected static int tileOrientation;
    protected static boolean lettersGoHorizontal;
    protected static boolean letterAHasEvenNumbers;
    
    // Coordinates as used in the ui.hexmap package
    protected int x;
    protected int y;
    
    // Map coordinates as printed on the game board
    protected String name;
    protected int row;
    protected int column;
    protected int letter;
    protected int number;
    
    public MapHex () {
    }
    
    public void configureFromXML(Element el) throws ConfigurationException
    {
        NamedNodeMap nnp = el.getAttributes();
        Pattern namePattern = Pattern.compile ("(\\D)(\\d+)");
        
        name = XmlUtils.extractStringAttribute(nnp, "name");
        Matcher m = namePattern.matcher(name);
        if (!m.matches()) {
            throw new ConfigurationException ("Invalid name format: "+name);
        }
        letter = m.group(1).charAt(0);
        try {
            number = Integer.parseInt(m.group(2));
        } catch (NumberFormatException e) {
            // Cannot occur!
        }
        
        /* Translate hex names (as on the board) to coordinates used for drawing. */
        if (lettersGoHorizontal) {
            row = number;
            column = letter - '@';
            if (tileOrientation == MapHex.EW) {
                // Tiles with flat EW sides, letters go horizontally.
                // Example: 1841 (NOT TESTED, PROBABLY WRONG).
                x = column;
                y = row / 2;
            } else {
                // Tiles with flat NS sides, letters go horizontally.
                // Tested for 1856.
                x = column;
                y = (row+1) / 2;
             }
        } else {
            row = letter - '@';
            column = number;
            if (tileOrientation == MapHex.EW) {
                // Tiles with flat EW sides, letters go vertically.
                // Most common case.
                // Tested for 1830.
                x = column / 2;
                y = row;
            } else {
                // Tiles with flat NS sides, letters go vertically.
                // No known examples (NOT TESTED, PROBABLY WRONG).
                x = row;
                y = column / 2;
           }
        }
        
   }    
    
    public static void setTileOrientation (int orientation) {
        tileOrientation = orientation;
    }
    
    public static int getTileOrientation () {
        return tileOrientation;
    }
    
    public static void setLettersGoHorizontal (boolean b) {
        lettersGoHorizontal = b;
    }
    
    /**
     * @return Returns the letterAHasEvenNumbers.
     */
    public static boolean hasLetterAEvenNumbers() {
        return letterAHasEvenNumbers;
    }
    /**
     * @param letterAHasEvenNumbers The letterAHasEvenNumbers to set.
     */
    public static void setLetterAHasEvenNumbers(boolean letterAHasEvenNumbers) {
        MapHex.letterAHasEvenNumbers = letterAHasEvenNumbers;
    }
    /**
     * @return Returns the lettersGoHorizontal.
     */
    public static boolean isLettersGoHorizontal() {
        return lettersGoHorizontal;
    }
    /**
     * @return Returns the column.
     */
    public int getColumn() {
        return column;
    }
    /**
     * @return Returns the row.
     */
    public int getRow() {
        return row;
    }
    public String getName() {
        return name;
    }
    
    public int getX () {
        return x;
    }

    public int getY () {
        return y;
    }
}
