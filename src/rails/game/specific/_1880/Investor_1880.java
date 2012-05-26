/**
 * 
 */
package rails.game.specific._1880;

import rails.game.PublicCompany;

/**
 * @author Martin 2011/04/11
 *
 */
public class Investor_1880 extends PublicCompany {
/*
 * Investors in 1880 get chosen at start after the initial starting package is sold out. They get one share from a new company 
 * TODO: Make sure that dividends aren't accumulated on the investors
    
*/
    protected boolean canOwnShare=true;
    
    protected int maxPercofShares=1;
    
    protected boolean hasStockPrice=false;
    
    protected boolean hasParPrice=false;
    
    protected PublicCompany linkedCompany;  // An Investor is always linked to a (exactly one) Public Major Company..
    
    /* Investors in 1880 operate with the newest train model on lease from the bank for zero costs.
    */
    protected boolean canBorrowTrain=true;
    
      
    /**
     * 
     */
    public Investor_1880() {
        super();    
    }
    
    public boolean canOwnShare(){
        return canOwnShare;
    }
    
    public int maxPercofShares(){
        return maxPercofShares;
    }
    public boolean hasStockPrice(){
        return hasStockPrice;
    }
    
    public boolean hasParPrice(){
        return hasParPrice;
    }
    
    public PublicCompany getLinkedCompany(){
        return linkedCompany;
    }
    
    public boolean setLinkedCompany(PublicCompany linkedCompany){
        if (linkedCompany != null){
            //Check if Company is valid i.e. not Closed maybe check if theres already the President sold and just the president...
            if(!linkedCompany.isClosed()){
                this.linkedCompany=linkedCompany;
                return true;}
            }
        return false; 
        }
    
}
