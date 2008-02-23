/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyTrain.java,v 1.8 2008/02/23 20:54:39 evos Exp $
 * 
 * Created on 20-May-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.Portfolio;
import rails.game.Train;
import rails.game.TrainI;
import rails.game.special.SpecialProperty;
import rails.game.special.SpecialTrainBuy;
import rails.util.Util;

/**
 * @author Erik Vos
 */
public class BuyTrain extends PossibleORAction {

	// Initial settings
    transient private TrainI train;
    private String trainUniqueId;
    transient private Portfolio from;
    private String fromName;
    private int fixedCost = 0;
    transient private List<TrainI> trainsForExchange = null;
    private String[] trainsForExchangeUniqueIds;
    private boolean presidentMustAddCash = false;
    private boolean presidentMayAddCash = false;
    private int presidentCashToAdd = 0;
    
    transient private SpecialTrainBuy specialProperty = null;
    private int specialPropertyId = 0;
    
    // User settings
    private int pricePaid = 0;
    private int addedCash = 0;
    transient private TrainI exchangedTrain = null;
    private String exchangedTrainUniqueId;
    private boolean forcedExchange = false;
    
    public static final long serialVersionUID = 2L;

    public BuyTrain (TrainI train, Portfolio from, int fixedCost) {
    	
        this.train = train;
        this.trainUniqueId = train.getUniqueId();
        this.from = from;
        this.fromName = from.getName();
        this.fixedCost = fixedCost;
    }
    
    public BuyTrain setTrainsForExchange (List<TrainI> trains) {
        trainsForExchange = trains;
        if (trains != null) {
        	trainsForExchangeUniqueIds = new String[trains.size()];
	        for (int i=0; i<trains.size(); i++) {
	            trainsForExchangeUniqueIds[i] = trains.get(i).getName(); // TODO: Must be replaced by unique Ids
	        }
        }
        return this;
    }
    
    public BuyTrain setPresidentMustAddCash (int amount) {
        presidentMustAddCash = true;
        presidentCashToAdd = amount;
        return this;
    }
    
    public BuyTrain setPresidentMayAddCash (int amount) {
        presidentMayAddCash = true;
        presidentCashToAdd = amount;
        return this;
    }
    
    public BuyTrain setForcedExchange (boolean value) {
    	forcedExchange = value;
    	return this;
    }
    
    /**
     * @return Returns the specialProperty.
     */
    public SpecialTrainBuy getSpecialProperty() {
        return specialProperty;
    }
    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialTrainBuy specialProperty) {
        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
    }
    
    public boolean hasSpecialProperty() {
    	return specialProperty != null;
    }
    
    public TrainI getTrain() {
        return train;
    }
    
    public Portfolio getFromPortfolio () {
    	return from;
    }
    
    public int getFixedCost () {
        return fixedCost;
    }
    
    public boolean isForExchange () {
        return trainsForExchange != null && !trainsForExchange.isEmpty();
    }
    
    public List<TrainI> getTrainsForExchange () {
        return trainsForExchange;
    }
    
    public boolean isForcedExchange() {
    	return forcedExchange;
    }
    
    public boolean mustPresidentAddCash () {
        return presidentMustAddCash;
    }
    
    public boolean mayPresidentAddCash () {
        return presidentMayAddCash;
    }
    
    public int getPresidentCashToAdd () {
        return presidentCashToAdd;
    }
    
    public Portfolio getHolder () {
        return train.getHolder();
    }
    
    public CashHolder getOwner () {
        return train.getOwner();
    }
    
    public int getAddedCash() {
		return addedCash;
	}

	public void setAddedCash(int addedCash) {
		this.addedCash = addedCash;
	}

	public int getPricePaid() {
		return pricePaid;
	}

	public void setPricePaid(int pricePaid) {
		this.pricePaid = pricePaid;
	}
	
	public TrainI getExchangedTrain() {
		return exchangedTrain;
	}

	public void setExchangedTrain(TrainI exchangedTrain) {
		this.exchangedTrain = exchangedTrain;
        if (exchangedTrain != null) this.exchangedTrainUniqueId = exchangedTrain.getName();// TODO: Must be replaced by unique Id
	}

	public String toString() {
		
		StringBuffer b = new StringBuffer();
		b.append (company.getName());
		b.append (": buy ").append(train.getName());
		b.append("-train from ").append(from.getName());
		b.append (" for ").append(Bank.format(fixedCost));
		if (specialProperty != null) {
			b.append(" using ").append(specialProperty.getCompany().getName());
		}
		if (isForExchange()) {
			b.append (forcedExchange ? " (forced exchange)" : " (exchanged)");
		}
		if (presidentMustAddCash) b.append(" must add cash ").append(Bank.format(presidentCashToAdd));
		else if (presidentMayAddCash) b.append(" may add cash up to ").append(Bank.format(presidentCashToAdd));
		
		return b.toString();
    }
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof BuyTrain)) return false;
        BuyTrain a = (BuyTrain) action;
        return a.train == train
        	&& a.from == from
            && a.fixedCost == fixedCost
            && a.trainsForExchange == trainsForExchange;
    }
    
    /** Deserialize */
	private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
		train = Train.getByUniqueId(trainUniqueId);
		from = Portfolio.getByName(fromName);
		if (trainsForExchangeUniqueIds != null
				&& trainsForExchangeUniqueIds.length > 0) {
			trainsForExchange = new ArrayList<TrainI>();
			for (int i=0; i<trainsForExchangeUniqueIds.length; i++) {
				trainsForExchange.add (Train.getByUniqueId(trainsForExchangeUniqueIds[i]));
			}
		}
		
		if (specialPropertyId  > 0) {
			specialProperty = (SpecialTrainBuy) SpecialProperty.getByUniqueId (specialPropertyId);
		}

		if (Util.hasValue(exchangedTrainUniqueId)) {
			exchangedTrain = Train.getByUniqueId(exchangedTrainUniqueId);
		}
	}



}
