package rails.game;

public interface TrainI
{

	/**
	 * @return Returns the cost.
	 */
	public int getCost();

	/**
	 * @return Returns the number of major stops cities, off-board, perhaps
	 *         towns.
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
	 * @return Returns the train type.
	 */
	public TrainTypeI getType();

	public String getName();

	public Portfolio getHolder();

	public CashHolder getOwner();

	public void setHolder(Portfolio newHolder);

	public void setRusted();

	public boolean canBeExchanged();

}
