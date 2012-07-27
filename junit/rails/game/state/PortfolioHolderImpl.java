package rails.game.state;

class PortfolioHolderImpl extends AbstractItem implements PortfolioHolder {
    
    private PortfolioHolderImpl(Owner parent, String id) {
        super (parent, id);
    }
    
    static PortfolioHolderImpl create(Owner parent, String id) {
        return new PortfolioHolderImpl(parent, id);
    }
    
    public Owner getParent() {
        return (Owner)super.getParent();
    }
}

