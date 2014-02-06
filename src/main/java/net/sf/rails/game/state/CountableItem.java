package net.sf.rails.game.state;

import com.google.common.base.Preconditions;

public abstract class CountableItem<T extends Countable> extends AbstractItem implements Countable {
    
    private final Class<T> type;
    private final WalletManager wm;
    
    /**
     * Initializes CountableItem
     * @param parent parent is usually a factory that creates the CountableItem(s)  
     * @param id identifier of the item
     * @param type indicates the class used for the Wallets to store this type of CountableItems
     */
    protected CountableItem(Item parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.wm = getRoot().getStateManager().getWalletManager();
    }

    public void move(Owner from, int amount, Owner to) {
        Preconditions.checkArgument(from != to, 
                "New Owner identical to the existing owner" + to);
        // TODO: Currently we still allow zero amounts
        // as e.g. during withhold the zero amount is paid
        Preconditions.checkArgument(amount >= 0, 
                "Amount to move restricted to positive numbers");
        
        // add to new wallet
        Wallet<T> newWallet = wm.getWallet(type, to);
        Preconditions.checkArgument(newWallet != null, "No Wallet available for owner " + to);
        new WalletChange<T>(newWallet, type.cast(this), amount);
        
        if (from != wm.getUnkownOwner()) {
            Wallet<T> oldWallet = wm.getWallet(type, from);
            Preconditions.checkArgument(oldWallet != null, "No Wallet available for owner" + from);
            new WalletChange<T>(oldWallet, type.cast(this), -amount);
        }
    }
    
    public int compareTo(Countable other) {
        return this.getId().compareTo(other.getId());
    }

}
