/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyTrain.java,v 1.1 2007/07/05 17:57:54 evos Exp $
 * 
 * Created on 20-May-2006
 * Change Log:
 */
package rails.game.action;

import java.util.List;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.Portfolio;
import rails.game.TrainI;

/**
 * @author Erik Vos
 */
public class BuyTrain extends PossibleORAction {

	// Initial settings
    private TrainI train;
    private Portfolio from;
    private int fixedCost = 0;
    private List<TrainI> trainsForExchange = null;
    private boolean presidentMustAddCash = false;
    private boolean presidentMayAddCash = false;
    private int presidentCashToAdd = 0;
    
    // User settings
    private int pricePaid = 0;
    private int addedCash = 0;
    private TrainI exchangedTrain = null;
    
    public BuyTrain (TrainI train, Portfolio from, int fixedCost) {
    	
        this.train = train;
        this.from = from;
        this.fixedCost = fixedCost;
    }
    
    public BuyTrain setTrainsForExchange (List<TrainI> trains) {
        trainsForExchange = trains;
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
	}

	public String toString() {
		
		StringBuffer b = new StringBuffer();
		b.append ("Buy ").append(train.getName());
		b.append("-train from ").append(from.getName());
		b.append (" for ").append(Bank.format(fixedCost));
		if (isForExchange()) b.append (" (exchanged)");
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

}
