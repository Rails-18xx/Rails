package rails.game.state;

public final class PortfolioChange<T extends Ownable> extends Change {

    private final Portfolio<T> portfolio;
    private final T item;
    private final boolean intoPortfolio;

    PortfolioChange(Portfolio<T> portfolio, T item, boolean intoPortfolio) {
        this.portfolio = portfolio;
        this.item = item;
        this.intoPortfolio = intoPortfolio;
        super.init(portfolio);
    }
    
    @Override 
    void execute() {
        portfolio.change(item, intoPortfolio);
    }

    @Override 
    void undo() {
        portfolio.change(item, !intoPortfolio);
    }

    @Override 
    Portfolio<? super T> getState() {
        return portfolio;
    }
    
    @Override
    public String toString() {
        if (intoPortfolio) {
            return "Change for " + portfolio + ": Add " + item;
        } else {
            return "Change for " + portfolio + ": Remove " + item;
        }
    }
    
    // external information
    public T getItem() {
        return item;
    }
    
    public boolean isIntoPortfolio() {
        return intoPortfolio;
    }

}
