/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialPropertyI.java,v 1.2 2005/11/27 20:59:24 evos Exp $
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
    
    public void setExercised ();
    public boolean isExercised();
    
    public boolean isSRProperty ();
    public boolean isORProperty ();

}
