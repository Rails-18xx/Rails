/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/WorthModel.java,v 1.1 2005/12/11 21:06:49 evos Exp $
 * 
 * Created on 11-Dec-2005
 * Change Log:
 */
package game.model;

import game.Bank;
import game.Player;

/**
 * @author Erik Vos
 */
public class WorthModel extends ModelObject {

    private Player owner;
    
    public WorthModel (Player owner) {
        this.owner = owner;
    }
    
    public String toString() {
        return Bank.format(owner.getWorth());
    }

}
