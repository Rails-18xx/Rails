/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/BuyableTrain.java,v 1.2 2006/05/21 18:48:03 evos Exp $
 * 
 * Created on 20-May-2006
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public class BuyableTrain {

    private TrainI train;
    private int fixedCost = 0;
    private boolean forExchange = false;
    private boolean presidentMustAddCash = false;
    private boolean presidentMayAddCash = false;
    private int presidentCashToAdd = 0;
    
    public BuyableTrain (TrainI train, int fixedCost) {
        
        this.train = train;
        this.fixedCost = fixedCost;
    }
    
    public BuyableTrain setForExchange () {
        forExchange = true;
        return this;
    }
    
    public BuyableTrain setPresidentMustAddCash (int amount) {
        presidentMustAddCash = true;
        presidentCashToAdd = amount;
        return this;
    }
    
    public BuyableTrain setPresidentMayAddCash (int amount) {
        presidentMayAddCash = true;
        presidentCashToAdd = amount;
        return this;
    }
    
    public TrainI getTrain() {
        return train;
    }
    
    public int getFixedCost () {
        return fixedCost;
    }
    
    public boolean isForExchange () {
        return forExchange;
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

}
