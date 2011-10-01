package rails.game.model;

import java.util.Collection;

import rails.game.Bank;
import rails.game.GameManager;

import com.google.common.collect.ImmutableList;

/**
 * A utility class that supports using Owner and Ownable objects
 * @author freystef
 *
 */

public class Owners {
    
    /**
     * Moves an ownable object to a newOwner
     * The oldOwner (if not null) automatically gets "unowned"
     * @param <E> type of the ownable object
     */
    public static <E extends Ownable> void move(E object, Owner newOwner) {
        // remove object from old owner
        if (object.getOwner() != null) {
            object.getOwner().removeObject(object);
        }
        // inform new owner about object
        newOwner.addObject(object);
        // set the new Owner for object
        object.setOwner(newOwner);
    }

    /**
     * Safely transfers several objects
     * @param <E> type of objects
     */
    public static <E extends Ownable> void move (Collection<E> objects, Owner newOwner) {
        // make Immutable copy first
        for (E object: ImmutableList.copyOf(objects)) {
            // then move each object
            move(object, newOwner);
        }
    }
    
    
    public static <E extends Ownable> void moveAll(Owner from, Owner to, Class<E> clazz) {
        for (E element:ImmutableList.copyOf(from.getHolder(clazz))) {
            from.removeObject(element);
            to.addObject(element);
        }
    }

    /**
     * Facilitates a move of cash. In this specific case either from or to may
     * be null, in which case the Bank is implied.
     */
    public static void cashMove(CashOwner from, CashOwner to, int amount) {
        from.getCashModel().add(-amount);
        to.getCashModel().add(amount);
    }
    
    public static void cashMoveToBank(CashOwner from, int amount) {
        // TODO: get this from the GameContext
        Bank bank = GameManager.getInstance().getBank();
        cashMove(from, bank, amount);
    }

    public static void cashMoveFromBank(CashOwner to, int amount) {
        // TODO: get this from the GameContext
        Bank bank = GameManager.getInstance().getBank();
        cashMove(bank, to, amount);
    }

}
