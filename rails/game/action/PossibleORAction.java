/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/PossibleORAction.java,v 1.1 2007/07/05 17:57:54 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.PublicCompanyI;
import rails.game.RoundI;

/**
 * PossibleAction is the superclass of all classes that describe an allowed user action
 * (such as laying a tile or dropping a token on a specific hex, buying a train etc.).
 * @author Erik Vos
 */
/* Or should this be an interface? We will see. */
public abstract class PossibleORAction extends PossibleAction {

	PublicCompanyI company;

    /**
     * 
     */
    public PossibleORAction() {
    	
    	super();
    	RoundI round = GameManager.getInstance().getCurrentRound();
    	if (round instanceof OperatingRound) {
    		company = ((OperatingRound)round).getOperatingCompany();
    	}
    }
    
    public PublicCompanyI getCompany() {
    	return company;
    }
    
    public String getCompanyName() {
    	return company.getName();
    }
    
    /** To be used in the client (to enable safety check in the server) */
    public void setCompany(PublicCompanyI company) {
        this.company = company;
    }
}
