/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/StringState.java,v 1.4 2008/06/04 19:00:36 evos Exp $
 * 
 * Created on May 19, 2007
 * Change Log:
 */
package rails.game.state;

import rails.game.move.StateChange;

/**
 * @author Erik Vos
 */
public class StringState extends State {

    /**
     * @param name
     * @param clazz
     */
    public StringState(String name) {
        super(name, "");
    }

    /**
     * @param name
     * @param object
     */
    public StringState(String name, String value) {
        super(name, value);
    }

    public void set(String value) {
        super.set(value);
    }

    public void appendWithDelimiter(String value, String delimiter) {
        String oldValue = (String) object;
        if (oldValue == null) oldValue = "";
        StringBuffer newValue = new StringBuffer(oldValue);
        if (newValue.length() > 0) newValue.append(delimiter);
        newValue.append(value);
        new StateChange(this, newValue.toString());
    }

    public String stringValue() {
        return (String) object;
    }
}
