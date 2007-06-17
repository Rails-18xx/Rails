package rails.game.state;

public interface StateI {

    public String getName();
	public Object getObject ();
	public void setState (Object value);

}
