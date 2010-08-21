/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/StateI.java,v 1.5 2008/06/04 19:00:36 evos Exp $*/
package rails.game.state;

public interface StateI {

    public String getName();

    public Object get();

    public void setState(Object value);

}
