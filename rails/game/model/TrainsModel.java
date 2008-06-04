/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/TrainsModel.java,v 1.6 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import rails.game.Portfolio;
import rails.game.TrainManager;

public class TrainsModel extends ModelObject {

    private Portfolio portfolio;

    public static final int FULL_LIST = 0;
    public static final int ABBR_LIST = 1;

    public TrainsModel(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public String getText() {
        if (option == FULL_LIST) {
            return TrainManager.makeFullList(portfolio);
        } else if (option == ABBR_LIST) {
            return TrainManager.makeAbbreviatedList(portfolio);
        } else {
            return "";
        }
    }
}
