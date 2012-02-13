package rails.game.state;

/**
 * WalletManager stores links to all existing wallets
 * @author freystef
 */

public final class WalletManager extends AbstractItem {

    private final HashMultimapState<Item, Wallet<?>> wallets = HashMultimapState.create();
    

    static WalletManager create() {
        return new WalletManager();
    }
    
    @Override
    public WalletManager init(Item parent, String id) {
        super.init(parent, id);
        return this;
    }
    
    boolean addWallet(Wallet<?> w){
        return wallets.put(w.getParent(), w);
    }
    
    boolean removeWallet(Wallet<?> w){
        return wallets.remove(w.getParent(), w);
    }
    
}
