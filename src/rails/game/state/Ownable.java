package rails.game.state;

public interface Ownable extends Item {
   
    /**
     * move to new owner
     */
    public void moveTo(Owner newOwner);
    
    /**
     * @return the current owner
     */
    public Owner getOwner();
    
}
