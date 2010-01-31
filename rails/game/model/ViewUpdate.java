package rails.game.model;

import java.io.Serializable;
import java.util.*;

/**
 * ViewUpdate is a composite object that can be sent from a ModelObject (Observable)
 * to a View object (Observer).
 * <p> The current version has text, background colour and foreground colour.
 * Receiving view objects must be prepared to handle extensions.
 * @author VosE
 *
 */

public class ViewUpdate implements Serializable {

    protected Map<String, Object> updates = new HashMap<String, Object>(4);

    public static final String TEXT = "TEXT";
    public static final String BGCOLOUR = "BGCOLOUR";

    public static final long serialVersionUID = 1L;

    public ViewUpdate (String key, Object value) {
        addObject (key, value);
    }

    public ViewUpdate (String text) {
        addObject (TEXT, text);
    }

    /** Add an object.
     * Return this ViewUpdate to enable chaining.
     */
    public ViewUpdate addObject (String key, Object value) {
        updates.put(key, value);
        return this;
    }

    public Set<String> getKeys () {
        return updates.keySet();
    }

    public boolean hasKey (String key) {
        return updates.containsKey(key);
    }

    public Object getValue(String key) {
        return updates.get(key);
    }

    public String getText () {
        return (String) updates.get(TEXT);
    }

}
