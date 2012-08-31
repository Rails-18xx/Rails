package rails.game.state;

public interface Ownable extends Item, Comparable<Ownable> {
   
    /**
     * Moves the ownable (item) to the new owner  
     * @param newOwner the new Owner of the Item
     * @throws IllegalArgumentException if the new owner has no wallet which accepts the item
     * @throws IllegalArgumentException if the new owner is identical to the current one 
     */
    public void moveTo(Owner newOwner);
    
    /**
     * @return the current owner
     */
    public Owner getOwner();
    
}
