/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Util.java,v 1.22 2010/05/16 20:57:40 evos Exp $*/
package rails.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.ConfigurationException;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolder;

public final class Util {

    protected static Logger log;

    /**
     * No-args private constructor, to prevent (meaningless) construction of one
     * of these.
     */
    private Util() {}

    public static boolean hasValue(String s) {
        return s != null && !s.equals("");
    }

    public static String appendWithDelimiter(String s1, String s2,
            String delimiter) {
        StringBuffer b = new StringBuffer(s1 != null ? s1 : "");
        if (b.length() > 0) b.append(delimiter);
        b.append(s2);
        return b.toString();
    }

    public static String joinWithDelimiter (String[] sa, String delimiter) {
        StringBuilder b = new StringBuilder();
        for (String s : sa) {
            if (b.length() > 0) b.append(delimiter);
            b.append(s);
        }
        return b.toString();
    }

    public static String joinWithDelimiter (int[] sa, String delimiter) {
        StringBuilder b = new StringBuilder();
        for (int s : sa) {
            if (b.length() > 0) b.append(delimiter);
            b.append(s);
        }
        return b.toString();
    }

    public static int parseInt(String value) throws ConfigurationException {

        if (!hasValue(value)) return 0;

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid integer value: " + value,
                    e);
        }
    }

    public static boolean bitSet(int value, int bitmask) {

        return (value & bitmask) > 0;
    }

    public static int setBit(int value, int bitmask, boolean set) {

        if (set) {
            return bitmask | value;
        } else {
            System.out.println("Reset bit " + value + ": from " + bitmask
                    + " to " + (bitmask & ~value));
            return bitmask & ~value;
        }
    }

    
    /**
     * Convert java string to html string
     * Transformations:
     * - Converts \n to <br>
     */
    public static String convertToHtml(String javaString) {
        return javaString.replace("\n", "<br>");
    }
    
    
    
    /**
     * Safely move a list of objects from one holder to another, avoiding
     * ConcurrentModificationExceptions.
     *
     * @param from
     * @param to
     * @param objects
     */
    public static <T extends Moveable> void moveObjects(List<T> objects,
            MoveableHolder to) {

        if (objects == null || objects.isEmpty()) return;

        List<T> list = new ArrayList<T>();
        for (T object : objects) {
            list.add(object);
        }
        for (T object : list) {
            object.moveTo(to);
        }

    }

    /** Safely add an object to a List at a given position
     * @param objects The List to add the object to.
     * @param object The object to be added.
     * @param position The position at which the object must be added.
     * <br>If between 0 and the current list size (inclusive), the object is inserted at
     * the given position.<br>If -1, the object is inserted at the end.
     * <br>If any other value, nothing is done.
     * @return True if the insertion was successful.
     * */
    public static <T extends Moveable, U extends T> boolean addToList (List<T> objects,
            U object, int position) {
        if (objects == null || object == null) {
            return false;
        }
        if (position == -1) {
            return objects.add(object);
        } else if (position >= 0 && position <= objects.size()){
            objects.add(position, object);
            return true;
        }
        return false;
    }

    /**
     * Parse a boolean value for Rails
     * @param value string (allowed values for true: standard Boolean.parseBoolean and yes (after conversion to lowercase)
     * @return parsed value
     */
    public static boolean parseBoolean(String s) {
        if (s.toLowerCase().equals("yes")) {
            return true;
        }
        return Boolean.parseBoolean(s);
    }
    
    /**
     * Parse a colour definition string.
     * Currently supported formats:
     *   "RRGGBB" - each character being a hexadecimal digit
     *   "r,g,b"  - each letter representing an integer 0..255
     * @param s
     * @return
     */
    public static Color parseColour (String s) throws ConfigurationException{
        Color c = null;
        if (!Util.hasValue(s)) {
        } else if (s.indexOf(',') == -1) {
            // Assume hexadecimal RRGGBB
            try {
                c = new Color (Integer.parseInt(s, 16));
            } catch (NumberFormatException e) {
                getLogger().error ("Invalid hex RGB colour: "+s, e);
                throw new ConfigurationException (e);
            }
        } else {
            // Assume decimal r,g,b
            try {
                String[] parts = s.split(",");
                c = new Color (Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                getLogger().error ("Invalid nummeric RGB colour: "+s, e);
                throw new ConfigurationException (e);
            }
        }
        return c;
    }

    
    
    
    /**
     * Is a colour dark? (to check if FG colour needs be reversed)
     */
    public static boolean isDark(Color c) {
        if (c == null) return false;
        return Math.sqrt(0.241*c.getRed()*c.getRed()
                + 0.691*c.getBlue()*c.getBlue()
                + 0.068*c.getGreen()*c.getGreen()) < 128;
        // Copied this formula from
        // http://www.nbdtech.com/blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
    }

    public static Logger getLogger () {
        if (log == null) log = Logger.getLogger(Util.class.getPackage().getName());
        return log;

    }

    public static String lowerCaseFirst (String text) {
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }
}
