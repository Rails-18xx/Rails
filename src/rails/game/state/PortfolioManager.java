package rails.game.state;

/**
 * PortfolioManager stores links to all existing portfolios
 */

public final class PortfolioManager extends AbstractItem {

    private final HashMultimapState<Owner, Portfolio<?>> portfolios = HashMultimapState.create(this, "portfolios");
    
    private PortfolioManager(Item parent, String id) {
        super(parent, id);
    }
    
    static PortfolioManager create(StateManager parent, String id) {
        return new PortfolioManager(parent, id);
    }
    
    boolean addPortfolio(Portfolio<?> p){
        return portfolios.put(p.getOwner(), p);
    }
    
    boolean removePortfolio(Portfolio<?> p){
        return portfolios.remove(p.getOwner(), p);
    }
    
}
