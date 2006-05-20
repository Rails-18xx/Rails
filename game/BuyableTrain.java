/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/BuyableTrain.java,v 1.1 2006/05/20 20:54:14 evos Exp $
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
    private boolean mustRaiseCash = false;
    private int cashToRaise = 0;
    
    public BuyableTrain (TrainI train, int fixedCost) {
        
        this.train = train;
        this.fixedCost = fixedCost;
    }
    
    public BuyableTrain setForExchange () {
        forExchange = true;
        return this;
    }
    
    public BuyableTrain setMustRaiseCash (int amount) {
        mustRaiseCash = true;
        cashToRaise = amount;
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
    
    public boolean mustRaiseCashFor () {
        return mustRaiseCash;
    }
    
    public int getCashToRaise () {
        return cashToRaise;
    }
    
    public Portfolio getHolder () {
        return train.getHolder();
    }
    
    public CashHolder getOwner () {
        return train.getOwner();
    }

}
