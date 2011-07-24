package rails.game.state;

import com.google.common.collect.ImmutableList;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.GameManager;

public class MoveUtils{
    /**
     * Facilitates a move of cash. In this specific case either from or to may
     * be null, in which case the Bank is implied.
     *
     * @param from The cash payer (null implies the Bank).
     * @param to The cash payee (null implies the Bank).
     * @param amount
     */
    public static void cashMove(CashHolder from, CashHolder to, int amount) {
        // TODO: get this from the GameContext
        Bank bank = GameManager.getInstance().getBank();
        
        // define from and to
        from = from != null ? from : bank;
        to = to != null ? to : bank;
        
        from.addCash(-amount);
        to.addCash(amount);
    }
    
    /**
     * @param <E> type of object
     * @param from Stateful list for removal
     * @param to Stateful list for addition
     * @param element element to move
     */
    public static <E> void objectMove(E element, ArrayListState<E> from, ArrayListState<E> to) {
        from.remove(element);
        to.add(element);
    }
    
    public static <E> void objectMoveAll(ArrayListState<E> from, ArrayListState<E> to) {
        for (E element:ImmutableList.copyOf(from.view())) {
            from.remove(element);
            to.add(element);
        }
    }
    
    
    
}
