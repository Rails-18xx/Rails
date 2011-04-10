/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/EnumState.java,v 1.1 2010/02/06 23:48:25 evos Exp $*/
package rails.game.state;

public class EnumState<E extends Enum> extends State implements StateI {

    public EnumState(String name, E object) {
        super (name, object);
    }

    @SuppressWarnings("unchecked")
    public E value() {
        return (E)object;
    }
    
}
