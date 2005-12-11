/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/TrainsModel.java,v 1.1 2005/12/11 21:06:49 evos Exp $
 * 
 * Created on 11-Dec-2005
 * Change Log:
 */
package game.model;

import game.Portfolio;
import game.TrainManager;

/**
 * @author Erik Vos
 */
public class TrainsModel extends ModelObject {

    private Portfolio portfolio;
    
    public static final int FULL_LIST = 0;
    public static final int ABBR_LIST = 1;
    
    public TrainsModel (Portfolio portfolio) {
        this.portfolio = portfolio;
    }
    
    public ModelObject option (int i) {
        if (i == FULL_LIST || i == ABBR_LIST) option = i;
        return this;
    }
    
    public String toString() {
        if (option == FULL_LIST) {
            return TrainManager.makeFullList(portfolio);
        } else if (option == ABBR_LIST) {
            return TrainManager.makeAbbreviatedList(portfolio);
        } else {
            return "";
        }
    }


}
