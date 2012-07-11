package rails.game.state;

/**
 * Abstract class for a Formatter 
 */
public abstract class Formatter<T extends Observable> {
    
    private final T observable;
    
    protected Formatter(T observable) {
        this.observable = observable;
    }
    
    public abstract String observerText();
    
    public void addObserver(Observer observer) {
        observable.getStateManager().addObserver(observer, this);
    }
    
    public T getObservable() {
        return observable;
    }
}
