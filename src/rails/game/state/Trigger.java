package rails.game.state;

public interface Trigger {

    /**
     * Method that is called if something has changed
     */
    public void triggered(Observable observable, Change change);
    
    
}
