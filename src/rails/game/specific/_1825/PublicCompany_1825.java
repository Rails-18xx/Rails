package rails.game.specific._1825;

import rails.game.PublicCompany;
import rails.game.RailsItem;
import rails.game.state.IntegerState;

public final class PublicCompany_1825 extends PublicCompany {
    
    protected final IntegerState formationOrderIndex = IntegerState.create(this, "formationOrderIndex");

    public PublicCompany_1825(RailsItem parent, String id) {
        super(parent, id);    
    }
    
    public int getFormationOrderIndex() {
        return formationOrderIndex.value();
    }

    public void setFormationOrderIndex(int formationOrderIndex) {
        this.formationOrderIndex.set(formationOrderIndex);
    }

    @Override
    public void payout(int amount) {
        if (amount == 0) return;
        //Get current price
        int curSharePrice = currentPrice.getPrice().getPrice();
        // Move the token
        // Work out number of spaces to move by dividing amount by current share price and rounding
        // Move stock token a number of times equal to this multiplier
        if (hasStockPrice){
            float shareMultiplier = amount/(float)curSharePrice;
            if(shareMultiplier<1){
                shareMultiplier = Math.round(shareMultiplier);
            }else{
                shareMultiplier = (float)Math.floor(shareMultiplier);
            }
            
            for (int i = 0; i < shareMultiplier && i < 4; i++) {
                stockMarket.payOut(this);
            }
        }
    }

    @Override
    public void setFloated() {
        super.setFloated();
        
        //Need to find out if other corps exist at this IPO price
        //If so increment formationOrderIndex to control Operating sequence
        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (this.getIPOPrice() == company.getIPOPrice() && (this.getId().equals(company.getId()))){
                //Yes, we share IPO prices, has this other company been launched yet?
                if (company.hasFloated()){
                    //it has, we need to skip ahead of this corp
                    formationOrderIndex.add(1);
                }
            }
                
        }
    }
}
