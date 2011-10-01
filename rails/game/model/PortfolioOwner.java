package rails.game.model;

import rails.game.state.AbstractItem;
import rails.game.state.Item;

public abstract class PortfolioOwner extends AbstractItem implements Owner {
    
    private final Portfolio portfolio;
    
    public PortfolioOwner(Item parent, String id) {
        super(parent, id);
        portfolio = new Portfolio(this, "Portfolio");
    }
    
    public final <E extends Ownable> void addHolder(Holder<E> newHolder, Class<E> clazz) {
        portfolio.addHolder(newHolder, clazz);
    }
    
    public final <E extends Ownable> Holder<E> getHolder(Class<E> clazz) {
        return portfolio.getHolder(clazz);
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
