/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompanyI.java,v 1.8 2010/01/08 21:30:46 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.move.MoveableHolder;
import rails.game.special.SpecialPropertyI;

public interface PrivateCompanyI extends CompanyI, Certificate, MoveableHolder {

    public static final String TYPE_TAG = "Private";
    public static final String REVENUE = "revenue";

    /**
     * @return
     */
    public int getPrivateNumber();

    /**
     * @return
     */
    public int getBasePrice();

    /**
     * @return
     */
    public int getRevenue();

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getSpecialProperties();

    public List<MapHex> getBlockedHexes();

    public void setHolder(Portfolio portfolio);

    // Methods related to closure when special properties are exercised.
    public boolean closesIfAllExercised();
    public boolean closesIfAnyExercised();
    public boolean closesAtEndOfTurn();
    public void checkClosingIfExercised(boolean endOfOR);
}
