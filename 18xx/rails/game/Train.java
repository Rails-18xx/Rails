package rails.game;

public class Train implements TrainI
{

	protected TrainTypeI type;

	protected int majorStops;
	protected int minorStops;
	protected int cost;
	protected int cityScoreFactor;
	protected int townScoreFactor;
	protected int townCountIndicator;

	protected Portfolio holder;
	protected boolean rusted = false;
	// protected boolean canBeExchanged = false;

	protected static final Portfolio unavailable = Bank.getUnavailable();
	protected static final Portfolio ipo = Bank.getIpo();

	public Train(TrainTypeI type)
	{

		this.type = type;
		this.majorStops = type.getMajorStops();
		this.minorStops = type.getMinorStops();
		this.cost = type.getCost();
		this.cityScoreFactor = type.getCityScoreFactor();
		this.townScoreFactor = type.getTownScoreFactor();
		this.townCountIndicator = type.getTownCountIndicator();

		unavailable.addTrain(this);

	}

	/**
	 * @return Returns the cityScoreFactor.
	 */
	public int getCityScoreFactor()
	{
		return cityScoreFactor;
	}

	/**
	 * @return Returns the cost.
	 */
	public int getCost()
	{
		return cost;
	}

	/**
	 * @return Returns the majorStops.
	 */
	public int getMajorStops()
	{
		return majorStops;
	}

	/**
	 * @return Returns the minorStops.
	 */
	public int getMinorStops()
	{
		return minorStops;
	}

	/**
	 * @return Returns the townCountIndicator.
	 */
	public int getTownCountIndicator()
	{
		return townCountIndicator;
	}

	/**
	 * @return Returns the townScoreFactor.
	 */
	public int getTownScoreFactor()
	{
		return townScoreFactor;
	}

	/**
	 * @return Returns the type.
	 */
	public TrainTypeI getType()
	{
		return type;
	}

	public String getName()
	{
		return type.getName();
	}

	public Portfolio getHolder()
	{
		return holder;
	}

	public CashHolder getOwner()
	{
		return holder.getOwner();
	}

	/**
	 * Move the train to another Portfolio.
	 */
	public void setHolder(Portfolio newHolder)
	{
		holder = newHolder;
	}

	public void setRusted()
	{
		rusted = true;
		Portfolio.transferTrain(this, holder, Bank.getScrapHeap());
	}

	public boolean canBeExchanged()
	{
		return type.nextCanBeExchanged();
	}

}
