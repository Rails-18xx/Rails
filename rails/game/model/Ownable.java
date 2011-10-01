package rails.game.model;

public interface Ownable {
    
    /**
     * This moves the Ownable object to a new Owner
     */
    public void moveTo(Owner newOwner);

    /**
     * Sets a new owner
     */
    public void setOwner(Owner newOwner);
    
    /**
     * @return the current holder
     */
    public Owner getOwner();
}
