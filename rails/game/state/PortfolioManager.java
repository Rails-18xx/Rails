package rails.game.state;

/**
 * PortfolioManager stores links to all existing portfolios
 * @author freystef
 */

public final class PortfolioManager extends AbstractItem {

    private final HashMultimapState<Item, PortfolioMap<?>> portfolios = HashMultimapState.create();
    
    private PortfolioManager() {};
    
    static PortfolioManager create() {
        return new PortfolioManager();
    }
    
    @Override
    public PortfolioManager init(Item parent, String id) {
        super.init(parent, id);
        return this;
    }
    
    boolean addPortfolio(PortfolioMap<?> p){
        return portfolios.put(p.getParent(), p);
    }
    
    boolean removePortfolio(PortfolioMap<?> p){
        return portfolios.remove(p.getParent(), p);
    }
    
}
