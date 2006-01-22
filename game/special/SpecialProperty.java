/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialProperty.java,v 1.3 2006/01/22 21:09:57 evos Exp $
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
    PrivateCompanyI privateCompany;
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
    
    public void setCompany (PrivateCompanyI company) {
        this.privateCompany = company;
    }
    public PrivateCompanyI getCompany () {
        return privateCompany;
    }
    
    public void setExercised () {
        exercised = true;
        privateCompany.getPortfolio().updateSpecialProperties();
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
