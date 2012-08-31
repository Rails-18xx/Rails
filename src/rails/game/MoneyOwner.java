package rails.game;

import rails.game.model.WalletMoneyModel;
import rails.game.state.Owner;

public interface MoneyOwner extends Owner, RailsItem {
    
    public WalletMoneyModel getWallet();
    
    public int getCash();
    
}
