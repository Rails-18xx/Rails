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
	    super (name, 0);
	}
	
	public MoneyModel (String name, int value) {
		
		super (name, value);
	}
	
	public void set (int value) {
		
	    super.set (value);
	    
    	/* Set initialisation state only if it matters */ 
	    if (option == SUPPRESS_INITIAL_ZERO 
	    		&& initialised == null) {
		    initialised = new BooleanState (name+"_initialised", false);
	    }
	    if (initialised != null && !initialised.booleanValue()) {
	    	initialised.set(true);
	    }
	}

	public String getText()
	{
	    int amount = intValue();
	    if (amount == 0 
	            && (option == SUPPRESS_ZERO
	            	|| option == SUPPRESS_INITIAL_ZERO
	            		&& initialised != null 
	            		&& !initialised.booleanValue())) {
	        return "";
	    } else {
	        return Bank.format(amount);
	    }
	}
	
}
