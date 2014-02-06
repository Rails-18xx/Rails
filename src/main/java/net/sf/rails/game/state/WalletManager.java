package net.sf.rails.game.state;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * WalletManager stores links to all existing wallets
 */

public class WalletManager extends Manager {

    class WMKey<T extends Countable> {
        private final Class<T> type;
        private final Owner owner;
        
        private WMKey(Wallet<T> p) {
            this.type = p.getType();
            this.owner = p.getParent();
        }
        
        private WMKey(Class<T> type, Owner owner) {
            this.type = type;
            this.owner = owner;
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof WMKey)) return false; 
            WMKey<?> otherKey = (WMKey<?>)other;
            return Objects.equal(type, otherKey.type) && Objects.equal(owner, otherKey.owner);
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(type, owner);
        }
        
        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("Type", type).add("Owner", owner).toString();
        }
        
    }
    
    private final HashMapState<WMKey<? extends Countable>, Wallet<? extends Countable>> wallets = 
            HashMapState.create(this, "wallets");
    
    private final UnknownOwner unknown = UnknownOwner.create(this, "unknown");
    
    private WalletManager(Item parent, String id) {
        super(parent, id);
    }
    
    static WalletManager create(StateManager parent, String id) {
        return new WalletManager(parent, id);
    }

    public UnknownOwner getUnkownOwner() {
        return unknown;
    }

    /**
     * @param Wallet to add
     * @throws IllegalArgumentException if a Wallet of that type is already added
     */
    <T extends Countable> void addWallet(Wallet<T> Wallet){
        WMKey<T> key = new WMKey<T>(Wallet);
        Preconditions.checkArgument(!wallets.containsKey(key),
                "A Wallet of that type is defined for that owner already");
        wallets.put(key, Wallet);
    }
    
    /**
     * @param Wallet to remove
     */
    
    <T extends Countable> void removeWallet(Wallet<T> p){
        wallets.remove(new WMKey<T>(p));
    }

    /**
     * Returns the Wallet that stores items of specified type for the specified owner
     * @param type class of items stored in Wallet
     * @param owner owner of the Wallet requested
     * @return Wallet for type/owner combination (null if none is available)
     */
    // This suppress unchecked warnings is required as far I understand the literature on generics
    // however it should not be a problem as we store only type-safe Wallets
    @SuppressWarnings("unchecked")
    <T extends Countable> Wallet<T> getWallet(Class<T> type, Owner owner) {
        return (Wallet<T>) wallets.get(new WMKey<T>(type, owner));
    }

    // backdoor for testing
    <T extends Countable> WMKey<T> createWMKey(Class<T> type, Owner owner) {
        return this.new WMKey<T>(type, owner);
    }
    
}
