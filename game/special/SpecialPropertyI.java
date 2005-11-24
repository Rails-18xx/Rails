/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialPropertyI.java,v 1.1 2005/11/24 22:42:40 evos Exp $
 * 
 * Created on 24-Nov-2005
 * Change Log:
 */
package game.special;

import game.CompanyI;
import game.ConfigurableComponentI;

/**
 * @author Erik Vos
 */
public interface SpecialPropertyI extends ConfigurableComponentI {
    
    public void setCondition (String condition);
    public String getCondition();
    
    public void setCompany (CompanyI company);
    public CompanyI getCompany();
    
    public void setExercised ();
    public boolean isExercised();
    
    public boolean isSRProperty ();
    public boolean isORProperty ();

}
