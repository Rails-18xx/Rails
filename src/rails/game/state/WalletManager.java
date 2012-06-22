package rails.game.state;

/**
 * WalletManager stores links to all existing wallets
 * @author freystef
 */

public final class WalletManager extends AbstractItem {

    private final HashMultimapState<Item, Wallet<?>> wallets = HashMultimapState.create(this, "wallets");
    
    private WalletManager(Item parent, String id) {
        super(parent, id);
    }
    
    static WalletManager create(StateManager parent, String id) {
        return new WalletManager(parent, id);
    }
    
    boolean addWallet(Wallet<?> w){
        return wallets.put(w.getParent(), w);
    }
    
    boolean removeWallet(Wallet<?> w){
        return wallets.remove(w.getParent(), w);
    }
    
}
