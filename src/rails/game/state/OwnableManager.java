package rails.game.state;

/**
 * PortfolioManager stores links to all existing portfolios
 * @author freystef
 */

public final class OwnableManager<T extends Ownable<T>> extends AbstractItem {

    private final HashMultimapState<Owner, PortfolioMap<?>> portfolios = HashMultimapState.create();
    
    private OwnableManager() {};
    
    static <T extends Ownable<T>> OwnableManager<T> create() {
        return new OwnableManager<T>();
    }
    
    boolean addPortfolio(PortfolioMap<?> p){
        return portfolios.put(p.getOwner(), p);
    }
    
    boolean removePortfolio(PortfolioMap<?> p){
        return portfolios.remove(p.getOwner(), p);
    }
    
}
