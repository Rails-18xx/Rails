/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialPropertyI.java,v 1.3 2006/04/17 14:17:05 evos Exp $
 * 
 * Created on 24-Nov-2005
 * Change Log:
 */
package game.special;

import game.PrivateCompanyI;
import game.ConfigurableComponentI;

/**
 * @author Erik Vos
 */
public interface SpecialPropertyI extends ConfigurableComponentI {
    
    public void setCondition (String condition);
    public String getCondition();
    
    public void setCompany (PrivateCompanyI company);
    public PrivateCompanyI getCompany();
    
    public boolean isExecutionable();
    
    public void setExercised ();
    public boolean isExercised();
    
    public boolean isSRProperty ();
    public boolean isORProperty ();

}
