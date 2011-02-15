/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TrainTypeI.java,v 1.14 2010/03/04 22:08:09 evos Exp $ */
package rails.game;

import java.util.List;

public interface TrainTypeI
extends ConfigurableComponentI, Cloneable {

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor();

    /**
     * @return Returns the cost.
     */
    public int getCost();

    /**
     * @return Returns the countHexes.
     */
    public boolean countsHexes();

    /**
     * @return Returns the firstExchange.
     */
    public boolean nextCanBeExchanged();

    public void addToBoughtFromIPO();

    public int getNumberBoughtFromIPO();

    /**
     * @return Returns the firstExchangeCost.
     */
    public int getExchangeCost();

    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops();

    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops();

    /**
     * @return Returns the name.
     */
    public String getName();

    /**
     * @return Returns the releasedTrainType.
     */
    public List<TrainTypeI> getReleasedTrainTypes();

    /**
     * @return Returns the rustedTrainType.
     */
    public TrainTypeI getRustedTrainType();

    /**
     * @return Returns the startedPhaseName.
     */
    public String getStartedPhaseName();

    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator();

    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor();

    /**
     * @return Returns the available.
     */
    public boolean isAvailable();

    public boolean isObsoleting();

    public boolean hasInfiniteAmount();

    /**
     * @param available The available to set.
     */
    public void setAvailable(Bank bank);

    public void setRusted(Portfolio lastBuyingCompany);

    public boolean hasRusted();

    public String getReleasedTrainTypeNames();

    public String getRustedTrainTypeName();

    public boolean isPermanent();

    public void setPermanent(boolean permanent);

    public void setReleasedTrainTypes(List<TrainTypeI> releasedTrainTypes);

    public void setRustedTrainType(TrainTypeI rustedTrainType);

    public TrainI cloneTrain();

    public int getIndex();

    public TrainManager getTrainManager();
    public String getInfo();

}
