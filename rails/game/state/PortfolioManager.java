package rails.game.state;

import com.google.common.collect.HashMultimap;

/**
 * PortfolioManager stores links to all existing portfolios
 * @author freystef
 */

public class PortfolioManager extends Context {

    public static final String ID = "Portfolios";
    
    private final HashMultimap<Item, PortfolioMap<?>> portfolios = HashMultimap.create();
    
    private PortfolioManager() {
        super(ID);
    }

    static PortfolioManager create() {
        return new PortfolioManager();
    }
    
    @Override
    public PortfolioManager init(Item parent) {
        super.init(parent);
        return this;
    }
    
    boolean addPortfolio(PortfolioMap<?> p){
        return portfolios.put(p.getParent(), p);
    }
    
    boolean removePortfolio(PortfolioMap<?> p){
        return portfolios.remove(p.getParent(), p);
    }
    
}
