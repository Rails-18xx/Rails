/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/TrainTypeI.java,v 1.2 2005/10/11 17:35:29 wakko666 Exp $
 * 
 * Created on 19-Aug-2005
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
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
	public boolean isFirstExchange();

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

	public boolean hasInfiniteAmount();

	/**
	 * @param available
	 *            The available to set.
	 */
	public void setAvailable(boolean available);

	public void setRusted();

	public boolean getRusted();

	public String getReleasedTrainTypeName();

	public String getRustedTrainTypeName();

	public void setReleasedTrainType(TrainTypeI releasedTrainType);

	public void setRustedTrainType(TrainTypeI rustedTrainType);

}
