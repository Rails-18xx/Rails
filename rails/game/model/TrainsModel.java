/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/TrainsModel.java,v 1.8 2010/01/31 22:22:29 macfreek Exp $*/
package rails.game.model;

import rails.game.Portfolio;

public class TrainsModel extends ModelObject {

    private Portfolio portfolio;

    public static final int FULL_LIST = 0;
    public static final int ABBR_LIST = 1;

    public TrainsModel(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    @Override
    public String getText() {
        if (option == FULL_LIST) {
            return portfolio.makeFullListOfTrains();
        } else if (option == ABBR_LIST) {
            return portfolio.makeAbbreviatedListOfTrains();
        } else {
            return "";
        }
    }
}
