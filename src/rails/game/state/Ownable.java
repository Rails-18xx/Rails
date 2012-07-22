package rails.game.state;

public interface Ownable extends Item {
   
    /**
     * move to new owner
     */
    public void moveTo(Class<? extends Ownable> type, Owner newOwner);
    
    /**
     * @return the current owner
     */
    public Owner getOwner();
    
}
