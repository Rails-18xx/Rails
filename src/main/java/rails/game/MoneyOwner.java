package rails.game;

import rails.game.model.WalletMoneyModel;

public interface MoneyOwner extends RailsOwner {
    
    public WalletMoneyModel getWallet();
    
    public int getCash();
    
}
