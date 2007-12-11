/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Train.java,v 1.7 2007/12/11 20:58:33 evos Exp $ */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import rails.game.move.TrainMove;
import rails.game.state.BooleanState;

public class Train implements TrainI
{

	protected TrainTypeI type;

	protected int majorStops;
	protected int minorStops;
	protected int cost;
	protected int cityScoreFactor;
	protected int townScoreFactor;
	protected int townCountIndicator;
	
	protected String uniqueId;
	protected static Map<String, TrainI> trainMap
			= new HashMap<String, TrainI> (); 

	protected Portfolio holder;
    protected BooleanState obsolete;

	protected static final Portfolio unavailable = Bank.getUnavailable();
	protected static final Portfolio ipo = Bank.getIpo();

	public Train(TrainTypeI type, int index)
	{

		this.type = type;
		this.majorStops = type.getMajorStops();
		this.minorStops = type.getMinorStops();
		this.cost = type.getCost();
		this.cityScoreFactor = type.getCityScoreFactor();
		this.townScoreFactor = type.getTownScoreFactor();
		this.townCountIndicator = type.getTownCountIndicator();

		unavailable.addTrain(this);
		uniqueId = type.getName() + "_" + index;
		trainMap.put (uniqueId, this);
        
        obsolete = new BooleanState(uniqueId, false);
	}
	
	public static TrainI getByUniqueId (String id) {
		return trainMap.get (id);
	}
	
	public String getUniqueId () {
		return uniqueId;
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

    public boolean isObsolete () {
        return obsolete.booleanValue();
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
		new TrainMove (this, holder, Bank.getScrapHeap());
	}
    
    public void setObsolete () {
        obsolete.set(true);
    }

	public boolean canBeExchanged()
	{
		return type.nextCanBeExchanged();
	}

}
