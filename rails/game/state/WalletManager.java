package rails.game.state;

import com.google.common.collect.HashMultimap;

/**
 * WalletManager stores links to all existing wallets
 * @author freystef
 */

public class WalletManager extends Context {

    public static final String ID = "Wallets";

    private final HashMultimap<Item, Wallet<?>> wallets = HashMultimap.create();
    
    private WalletManager() {
        super(ID);
    }

    static WalletManager create() {
        return new WalletManager();
    }
    
    @Override
    public WalletManager init(Item parent) {
        super.init(parent);
        return this;
    }
    
    boolean addWallet(Wallet<?> w){
        return wallets.put(w.getParent(), w);
    }
    
    boolean removeWallet(Wallet<?> w){
        return wallets.remove(w.getParent(), w);
    }
    
}
