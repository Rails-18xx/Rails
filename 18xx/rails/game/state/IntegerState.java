/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/IntegerState.java,v 1.4 2009/09/04 18:35:53 evos Exp $
 *
 * Created on May 19, 2007
 * Change Log:
 */
package rails.game.state;

/**
 * @author Erik Vos
 */
public class IntegerState extends State {

    /**
     * @param name
     * @param clazz
     */
    public IntegerState(String name) {
        super(name, new Integer(0));
    }

    /**
     * @param name
     * @param object
     */
    public IntegerState(String name, int value) {
        super(name, new Integer(value));
    }

    public void set(int value) {
        super.set(new Integer(value));
    }

    public int add(int value) {
        int newValue = ((Integer) object).intValue() + value;
        set(newValue);
        return newValue;
    }

    public int intValue() {
        return ((Integer) object).intValue();
    }

    @Override
	public String getText() {
    	return ""+intValue();
    }
}
