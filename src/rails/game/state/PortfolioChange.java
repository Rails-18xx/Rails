package rails.game.state;

final class PortfolioChange<T extends Ownable> extends Change {

    private final Portfolio<T> in;
    private final Portfolio<T> out; // can be null
    private final T item;

    PortfolioChange(Portfolio<T> in, Portfolio<T> out, T item) {
        this.in = in;
        this.out = out;
        this.item = item;
        super.init(in);
    }
    
    @Override void execute() {
        in.change(item, true);
        if (out != null) {
            out.change(item,  false);
        }
    }

    @Override void undo() {
        in.change(item,  false);
        if (out != null) {
            out.change(item, true);
        }
    }

    @Override Portfolio<? super T> getState() {
        return in;
    }
    
    @Override
    public String toString() {
        if (out == null) {
            return "Change for " + in + ": Add " + item;
        } else {
            return "Change for " + in + ": Add + " + item + " from " + out;
        }
    }

}
