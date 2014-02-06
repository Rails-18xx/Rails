package net.sf.rails.game.state;

/**
 * Identifies items which are countable
 * They are stored inside wallets
 */

public interface Countable extends Item, Comparable<Countable> {

    /**
     * Moves the countable (item) from one to another owner
     * @param from the previous owner
     * @param int amount
     * @param to the new owner
     * @throws IllegalArgumentException if the new or the previous owner has no wallet which accepts the item
     * @throws IllegalArgumentException if the new owner is identical to the current one 
     */
    public void move(Owner from, int amount, Owner to);
    
}
