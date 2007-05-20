package rails.game.model;

import rails.game.Bank;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;

public class MoneyModel extends IntegerState
{
    public static final int SUPPRESS_ZERO = 1;
    public static final int SUPPRESS_INITIAL_ZERO = 2;
    private BooleanState initialised;

	public MoneyModel(String name)
	{
	    super (name);
	    initialised = new BooleanState (name+"Initialised", false);
	}
	
	public void set (int value) {
	    super.set (value);
	    if (!initialised.booleanValue()) initialised.set(true);
	}

	public String getText()
	{
	    int amount = intValue();
	    if (amount == 0 
	            && (option == SUPPRESS_ZERO
	            	|| !initialised.booleanValue() 
	            		&& option == SUPPRESS_INITIAL_ZERO)) {
	        return "";
	    } else {
	        return Bank.format(amount);
	    }
	}
	
}
