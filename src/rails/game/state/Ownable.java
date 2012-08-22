package rails.game.state;

public interface Ownable extends Item, Comparable<Ownable> {
   
    /**
     * Moves the ownable (item) to the new owner  
     * @param newOwner the new Owner of the Item
     * @throws NullPointerException if the new owner has no portfolio which accepts the item
     * @return false if newOwner is the existing owner
     */
    public boolean moveTo(Owner newOwner);
    
    /**
     * @return the current owner
     */
    public Owner getOwner();
    
}
