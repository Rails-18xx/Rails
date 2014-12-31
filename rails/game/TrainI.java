/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TrainI.java,v 1.8 2009/10/10 15:25:49 evos Exp $ */
package rails.game;

import rails.game.move.Moveable;

public interface TrainI extends Moveable {
    
    public void init(TrainCertificateType certType, TrainType type, String uniqueId);

    /**
     * @return Returns the cost.
     */
    public int getCost();

    /**
     * @return Returns the number of major stops cities, off-board, perhaps
     * towns.
     */
    public int getMajorStops();

    /**
     * @return Returns the minorStops (towns).
     */
    public int getMinorStops();

    /**
     * @return Returns the townCountIndicator (major, minor or not at all).
     */
    public int getTownCountIndicator();

    /**
     * @return Returns the cityScoreFactor (0 or 1).
     */
    public int getCityScoreFactor();

    /**
     * @return Returns the townScoreFactor (0 or 1).
     */
    public int getTownScoreFactor();
    
    /**
     * @return true => hex train (examples 1826, 1844), false => standard 1830 type train
     */
    public boolean isHTrain();

    /**
     * @return true => Express train (examples 1880)
     */
    public boolean isETrain();

    public void setType (TrainType type);
    /**
     * @return Returns the train type.
     */
    public TrainType getType();
    public TrainCertificateType getCertType();
    public TrainType getPreviousType();

    public String getName();

    public String getUniqueId();

    public Portfolio getHolder();

    public CashHolder getOwner();

    public boolean isObsolete();
    public boolean isPermanent();

    public void setHolder(Portfolio newHolder);

    // public void moveTo (Portfolio to);

    public void setRusted();

    public void setObsolete();

    public boolean canBeExchanged();

    public String toDisplay();
    
    public boolean isTradeable();
    public void setTradeable(boolean tradeable);

 
}
