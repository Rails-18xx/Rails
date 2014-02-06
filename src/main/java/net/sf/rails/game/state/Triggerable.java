package net.sf.rails.game.state;

public interface Triggerable {

    /**
     * Method that is called if something has changed
     */
    public void triggered(Observable observable, Change change);
    
    
}
