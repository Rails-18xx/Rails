/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/state/StateI.java,v 1.4 2007/10/05 22:02:31 evos Exp $*/
package rails.game.state;

public interface StateI {

    public String getName();
	public Object getObject ();
	public void setState (Object value);

}
