/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialProperty.java,v 1.1 2005/11/24 22:42:40 evos Exp $
 * 
 * Created on 24-Nov-2005
 * Change Log:
 */
package game.special;

import game.*;

/**
 * @author Erik Vos
 */
public abstract class SpecialProperty implements SpecialPropertyI {
    
    String condition;
    CompanyI company;
    int closingValue = 0;
    boolean exercised = false;
    boolean isSRProperty = false; 
    boolean isORProperty = false; 

    public void setCondition (String condition) {
        this.condition = condition;
    }
    public String getCondition() {
        return condition;
    }
    
    public void setCompany (CompanyI company) {
        this.company = company;
    }
    public CompanyI getCompany () {
        return company;
    }
    
    public void setExercised () {
        exercised = true;
    }
    public boolean isExercised() {
        return exercised;
    }
    
    public int getClosingValue() {
        return closingValue;
    }
    
    public boolean isSRProperty () {
        return isSRProperty;
    }

    public boolean isORProperty () {
        return isORProperty;
    }


}
