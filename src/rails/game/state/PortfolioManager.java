package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Objects;

/**
 * PortfolioManager stores links to all existing portfolios
 */

public final class PortfolioManager extends Manager implements DelayedItem {

    final class PMKey<T extends Ownable> {
        private final Class<T> type;
        private final Owner owner;
        
        private PMKey(Portfolio<T> p) {
            this.type = p.getType();
            this.owner = p.getOwner();
        }
        
        private PMKey(Class<T> type, Owner owner) {
            this.type = type;
            this.owner = owner;
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PMKey)) return false; 
            PMKey<?> otherKey = (PMKey<?>)other;
            return Objects.equal(type, otherKey.type) && Objects.equal(owner, otherKey.owner);
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(type, owner);
        }
    }
    
    private final HashMapState<PMKey<? extends Ownable>, Portfolio<? extends Ownable>> portfolios = 
            HashMapState.create(this, null);
    
    private final UnknownOwner unknown = UnknownOwner.create(this, "unknown");
    
    private PortfolioManager(Item parent, String id) {
        super(parent, id);
    }
    
    static PortfolioManager create(StateManager parent, String id) {
        return new PortfolioManager(parent, id);
    }

    UnknownOwner getUnkownOwner() {
        return unknown;
    }

    /**
     * @param portfolio to add
     * @throws IllegalArgumentException if a portfolio of that type is already added
     */
    <T extends Ownable> void addPortfolio(Portfolio<T> portfolio){
        PMKey<T> key = new PMKey<T>(portfolio);
        checkArgument(!portfolios.containsKey(key), "A portfolio of that type is defined for that owner already");
        portfolios.put(key, portfolio);
    }
    
    /**
     * @param portfolio to remove
     */
    
    <T extends Ownable> void removePortfolio(Portfolio<T> p){
        portfolios.remove(new PMKey<T>(p));
    }

    /**
     * Returns the Portfolio that stores items of specified type for the specified owner
     * @param type class of items stored in portfolio
     * @param owner owner of the portfolio requested
     * @return portfolio for type/owner combination (null if none is available)
     */
    // This suppress unchecked warnings is required as far I understand the literature on generics
    // however it should not be a problem as we store only type-safe portfolios
    @SuppressWarnings("unchecked")
    <T extends Ownable> Portfolio<T> getPortfolio(Class<T> type, Owner owner) {
        if (owner == unknown) return null;
        return (Portfolio<T>) portfolios.get(new PMKey<T>(type, owner));
    }

    // backdoor for testing
    <T extends Ownable> PMKey<T> createPMKey(Class<T> type, Owner owner) {
        return this.new PMKey<T>(type, owner);
    }
    
}
