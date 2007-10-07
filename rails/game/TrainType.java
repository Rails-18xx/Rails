/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TrainType.java,v 1.13 2007/10/07 20:14:54 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import rails.game.move.TrainMove;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.util.*;


public class TrainType implements TrainTypeI, ConfigurableComponentI, Cloneable
{

	public final static int TOWN_COUNT_MAJOR = 2;
	public final static int TOWN_COUNT_MINOR = 1;
	public final static int NO_TOWN_COUNT = 0;

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

	protected ArrayList<Train> trains = null;

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
		if (real)
			trains = new ArrayList<Train>();
	}

	/**
	 * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Tag tag) throws ConfigurationException
	{

		if (real)
		{
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

			// Now create the trains of this type
			if (infiniteAmount)
			{
				/*
				 * We create one train, but will add one more each time a train
				 * of this type is bought.
				 */
				trains.add(new Train(this, 0));
			}
			else
			{
				for (int i = 0; i < amount; i++)
				{
					trains.add(new Train(this, i));
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
	public int getAmount()
	{
		return amount;
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

	public void setRusted()
	{
	    rusted.set(true);
        for (TrainI train : trains) {
            train.setRusted();
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
