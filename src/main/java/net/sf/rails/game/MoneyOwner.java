package net.sf.rails.game;

import net.sf.rails.game.model.WalletMoneyModel;

public interface MoneyOwner extends RailsOwner {
    
    public WalletMoneyModel getWallet();
    
    public int getCash();
    
}
