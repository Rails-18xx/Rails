package rails.game.state;

/**
 * PortfolioManager stores links to all existing portfolios
 */

public final class PortfolioManager extends Manager implements DelayedItem {

    private final class PMKey<T extends Ownable> {
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
        // TODO: Write that correctly
        public boolean equals(Object other) {
            if (!(other instanceof PMKey)) return false; 
            return true;
        }
    }
    
    private final HashMapState<PMKey<? extends Ownable>, Portfolio<? extends Ownable>> portfolios = 
            HashMapState.create(this, null);
    
    private PortfolioManager(Item parent, String id) {
        super(parent, id);
    }
    
    static PortfolioManager create(StateManager parent, String id) {
        return new PortfolioManager(parent, id);
    }
    
    <T extends Ownable> void addPortfolio(Portfolio<T> p){
        portfolios.put(new PMKey<T>(p), p);
    }
    
    <T extends Ownable> void removePortfolio(Portfolio<T> p){
        portfolios.remove(new PMKey<T>(p));
    }
    
    // This suppress unchecked warnings is required as far I understand the literature on generics
    // however it should not be a problem as we store only type-safe portfolios
    @SuppressWarnings("unchecked")
    <T extends Ownable> Portfolio<T> getPortfolio(Class<T> type, Owner owner) {
        return (Portfolio<T>) portfolios.get(new PMKey<T>(type, owner));
    }
    
    <T extends Ownable> void moveItem(Class<T> type, T item, Owner newOwner) {
        Portfolio<T> oldPortfolio = getPortfolio(type, item.getOwner());
        Portfolio<T> newPortfolio = getPortfolio(type, newOwner);
        new PortfolioChange<T>(newPortfolio, oldPortfolio, item);
    }
    
    <T extends Ownable> void moveItem(Class<T> type, T item, Portfolio<T> newPortfolio) {
        Portfolio<T> oldPortfolio = getPortfolio(type, item.getOwner());
        new PortfolioChange<T>(newPortfolio, oldPortfolio, item);
    }
}
