package rails.game.model;

import rails.game.state.AbstractItem;
import rails.game.state.Item;

public abstract class PortfolioOwner extends AbstractItem implements Owner {
    
    private final Portfolio portfolio;
    
    public PortfolioOwner(String id) {
        super(id);
        portfolio = new Portfolio();
    }
    
    @Override
    public void init(Item parent){
        super.init(parent);
        portfolio.init(this);
    }
    
    public final <E extends Ownable> void addStorage(Storage<E> newHolder, Class<E> clazz) {
        portfolio.addStorage(newHolder, clazz);
    }
    
    public final <E extends Ownable> Storage<E> getStorage(Class<E> clazz) {
        return portfolio.getStorage(clazz);
    }

    public final <E extends Ownable> void addObject(E object) {
        portfolio.addObject(object);
    }

    public final <E extends Ownable> void removeObject(E object) {
        portfolio.removeObject(object);
    }
    
    public final Portfolio getPortfolio() {
        return portfolio;
    }
    
}
