/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TrainTypeI.java,v 1.5 2007/12/11 20:58:33 evos Exp $ */
package rails.game;

public interface TrainTypeI
{

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
	public int getFirstExchangeCost();

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
	public TrainTypeI getReleasedTrainType();

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
	 * @param available
	 *            The available to set.
	 */
	public void setAvailable();

	public void setRusted(Portfolio lastBuyingCompany);

	public boolean hasRusted();

	public String getReleasedTrainTypeName();

	public String getRustedTrainTypeName();

	public void setReleasedTrainType(TrainTypeI releasedTrainType);

	public void setRustedTrainType(TrainTypeI rustedTrainType);

}
