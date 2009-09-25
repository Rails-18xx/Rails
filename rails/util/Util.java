/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Util.java,v 1.15 2009/09/25 19:13:01 evos Exp $*/
package rails.util;

import java.util.ArrayList;
import java.util.List;

import rails.game.ConfigurationException;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolderI;

public final class Util {
    // protected static Logger log = Game.getLogger();

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
     * Safely move objects from one holder to another, avoiding
     * ConcurrentModificationExceptions.
     *
     * @param from
     * @param to
     * @param objects
     */
    public static <T extends Moveable> void moveObjects(List<T> objects,
            MoveableHolderI to) {

        if (objects == null || objects.isEmpty()) return;

        List<T> list = new ArrayList<T>();
        for (T object : objects) {
            list.add(object);
        }
        for (T object : list) {
            object.moveTo(to);
        }

    }
}
