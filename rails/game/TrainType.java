/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TrainType.java,v 1.15 2007/12/21 21:18:12 evos Exp $ */
package rails.game;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rails.game.move.TrainMove;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.util.*;


public class TrainType implements TrainTypeI, ConfigurableComponentI, Cloneable
{

	public final static int TOWN_COUNT_MAJOR = 2;
	public final static int TOWN_COUNT_MINOR = 1;
	public final static int NO_TOWN_COUNT = 0;

    protected String trainClassName = "rails.game.Train";
    protected Class trainClass;

	protected String name;
	protected int amount;
	protected boolean infiniteAmount = false;

	private String reachBasis = "stops";
	protected boolean countHexes = false;

	private String countTowns = "major";
	protected int townCountIndicator = TOWN_COUNT_MAJOR;

	private String scoreTowns = "yes";
	protected int townScoreFactor = 1;

	private String scoreCities = "single";
	protected int cityScoreFactor = 1;

	protected boolean firstCanBeExchanged = false;
	protected IntegerState numberBoughtFromIPO; 
    
    protected boolean obsoleting = false;

	private boolean real; // Only to determine if top-level attributes must be
	// read.

	protected int cost;
	protected int majorStops;
	protected int minorStops;
	protected int firstExchangeCost;

	protected String startedPhaseName = null;
	// Phase startedPhase;

	private String rustedTrainTypeName = null;
	protected TrainTypeI rustedTrainType = null;

	private String releasedTrainTypeName = null;
	protected TrainTypeI releasedTrainType = null;

	protected ArrayList<TrainI> trains = null;

	protected BooleanState available;
	protected BooleanState rusted;

	protected static Logger log = Logger.getLogger(TrainType.class.getPackage().getName());

	/**
	 * @param real
	 *            False for the default type, else real. The default type does
	 *            not have top-level attributes.
	 */
	public TrainType(boolean real)
	{
		this.real = real;
	}

	/**
	 * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Tag tag) throws ConfigurationException
	{
	    trainClassName = tag.getAttributeAsString("class", trainClassName);
	    try {
            trainClass = Class.forName(trainClassName);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException ("Class "+trainClassName+"not found", e);
        }

		if (real)
		{
            trains = new ArrayList<TrainI>();
            
			// Name
            name = tag.getAttributeAsString("name");
			if (name == null)
			{
				throw new ConfigurationException(LocalText.getText("NoNameSpecified"));
			}

			// Cost
            cost = tag.getAttributeAsInteger("cost");
			if (cost == 0)
			{
				throw new ConfigurationException(LocalText.getText("InvalidCost"));
			}

			// Amount
            amount = tag.getAttributeAsInteger("amount");
			if (amount == -1)
			{
				infiniteAmount = true;
			}
			else if (amount <= 0)
			{
				throw new ConfigurationException(LocalText.getText("InvalidAmount"));
			}

			// Major stops
            majorStops = tag.getAttributeAsInteger("majorStops");
			if (majorStops == 0)
			{
				throw new ConfigurationException(LocalText.getText("InvalidStops"));
			}

			// Minor stops
            minorStops = tag.getAttributeAsInteger("minorStops");

			// Phase started
            startedPhaseName = tag.getAttributeAsString("startPhase", "");

			// Train type rusted
            rustedTrainTypeName = tag.getAttributeAsString("rustedTrain");
            
			// Other train type released for buying
            releasedTrainTypeName = tag.getAttributeAsString("releasedTrain");
            
            // Can run as obsolete train
            obsoleting = tag.getAttributeAsBoolean("obsoleting");

        }
		else
		{
			name = "";
			amount = 0;
		}

		// Reach
		Tag reachTag = tag.getChild("Reach");
		if (reachTag != null)
		{
			// Reach basis
			reachBasis = reachTag.getAttributeAsString("base", reachBasis);

			// Are towns counted (only relevant is reachBasis = "stops")
			countTowns = reachTag.getAttributeAsString("countTowns", countTowns);
		}

		// Score
		Tag scoreTag = tag.getChild("Score");
		if (scoreTag != null)
		{
			// Reach basis
			scoreTowns = scoreTag.getAttributeAsString("scoreTowns", scoreTowns);

			// Are towns counted (only relevant is reachBasis = "stops")
			scoreCities = scoreTag.getAttributeAsString("scoreCities", scoreCities);
		}

		// Exchangeable
		Tag swapTag = tag.getChild("ExchangeFirst");
		if (swapTag != null)
		{
			firstExchangeCost = swapTag.getAttributeAsInteger("cost", 0);
			firstCanBeExchanged = (firstExchangeCost > 0);
		}

		if (real)
		{

			// Check the reach and score values
			countHexes = reachBasis.equals("hexes");
			townCountIndicator = countTowns.equals("no") ? NO_TOWN_COUNT
					: minorStops > 0 ? TOWN_COUNT_MINOR : TOWN_COUNT_MAJOR;
			cityScoreFactor = scoreCities.equals("double") ? 2 : 1;
			townScoreFactor = scoreTowns.equals("yes") ? 1 : 0;
			// Actually we should meticulously check all values....
			
			//log.debug("Train type "+name+": class "+trainClassName);

			// Now create the trains of this type
            TrainI train;
			if (infiniteAmount)
			{
				/*
				 * We create one train, but will add one more each time a train
				 * of this type is bought.
				 */
                try {
                    train = (TrainI) trainClass.newInstance();
                } catch (InstantiationException e) {
                    throw new ConfigurationException("Cannot instantiate class "+trainClassName, e);
                } catch (IllegalAccessException e) {
                    throw new ConfigurationException("Cannot access class "+trainClassName+"constructor", e);
                }
                train.init(this, 0);
				trains.add(train);
			}
			else
			{
				for (int i = 0; i < amount; i++)
				{
	                try {
	                    train = (TrainI) trainClass.newInstance();
	                } catch (InstantiationException e) {
	                    throw new ConfigurationException("Cannot instantiate class "+trainClassName, e);
	                } catch (IllegalAccessException e) {
	                    throw new ConfigurationException("Cannot access class "+trainClassName+"constructor", e);
	                }
                    train.init(this, i);
                    trains.add(train);
				}
			}
		}
		
		// Final initialisations
		numberBoughtFromIPO	= new IntegerState (name+"-trains_Bought", 0);
        available = new BooleanState (name+"-trains_Available", false);
        rusted = new BooleanState (name+"-trains_Rusted", false);
	}

	/**
	 * @return Returns the amount.
	 */
	/*
	public int getAmount()
	{
		return amount;
	}
	*/

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
	 * @return Returns the countHexes.
	 */
	public boolean countsHexes()
	{
		return countHexes;
	}

	/**
	 * @return Returns the firstExchange.
	 */
	public boolean nextCanBeExchanged()
	{
		return firstCanBeExchanged && numberBoughtFromIPO.intValue() == 0;
	}

	public void addToBoughtFromIPO()
	{
		numberBoughtFromIPO.add(1);
	}

	public int getNumberBoughtFromIPO()
	{
		return numberBoughtFromIPO.intValue();
	}

	/**
	 * @return Returns the firstExchangeCost.
	 */
	public int getFirstExchangeCost()
	{
		return firstExchangeCost;
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
	 * @return Returns the name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return Returns the releasedTrainType.
	 */
	public TrainTypeI getReleasedTrainType()
	{
		return releasedTrainType;
	}

	/**
	 * @return Returns the rustedTrainType.
	 */
	public TrainTypeI getRustedTrainType()
	{
		return rustedTrainType;
	}

	/**
	 * @return Returns the startedPhaseName.
	 */
	public String getStartedPhaseName()
	{
		return startedPhaseName;
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
	 * @return Returns the releasedTrainTypeName.
	 */
	public String getReleasedTrainTypeName()
	{
		return releasedTrainTypeName;
	}

	/**
	 * @return Returns the rustedTrainTypeName.
	 */
	public String getRustedTrainTypeName()
	{
		return rustedTrainTypeName;
	}
    
    public boolean isObsoleting() {
        return obsoleting;
    }

	/**
	 * @param releasedTrainType
	 *            The releasedTrainType to set.
	 */
	public void setReleasedTrainType(TrainTypeI releasedTrainType)
	{
		this.releasedTrainType = releasedTrainType;
	}

	/**
	 * @param rustedTrainType
	 *            The rustedTrainType to set.
	 */
	public void setRustedTrainType(TrainTypeI rustedTrainType)
	{
		this.rustedTrainType = rustedTrainType;
	}

	/**
	 * @return Returns the available.
	 */
	public boolean isAvailable()
	{
		return available.booleanValue();
	}

	/**
	 * Make a train type available for buying by public companies.
	 */
	public void setAvailable()
	{
		available.set(true);

		for (TrainI train : trains) 
		{
			new TrainMove (train, 
					Bank.getUnavailable(),
					Bank.getIpo());
		}
	}

	public boolean hasInfiniteAmount()
	{
		return infiniteAmount;
	}

	public void setRusted(Portfolio lastBuyingCompany)
	{
	    rusted.set(true);
        for (TrainI train : trains) {
            if (obsoleting && train.getHolder() != lastBuyingCompany) {
                log.debug("Train " + train.getUniqueId()
                        + " (owned by " +train.getHolder().getName()
                        + ") obsoleted");
                train.setObsolete();
                train.getHolder().getTrainsModel().update();
            } else {
                log.debug("Train " + train.getUniqueId()
                        + " (owned by " +train.getHolder().getName()
                        + ") rusted");
                train.setRusted();
            }
        }
	}

	public boolean hasRusted()
	{
		return rusted.booleanValue();
	}

	public Object clone()
	{

		Object clone = null;
		try
		{
			clone = super.clone();
			((TrainType) clone).real = true;
		}
		catch (CloneNotSupportedException e)
		{
			log.fatal ("Cannot clone traintype " + name, e);
			return null;
		}

		return clone;
	}

}
