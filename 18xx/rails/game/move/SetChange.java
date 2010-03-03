/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/SetChange.java,v 1.1 2010/03/03 00:45:09 stefanfrey Exp $
 *
 * Created on 19-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.Set;

/**
 * This Move class handles the addition or removal of set elements
 *
 * @author Stefan Frey
 */
public class SetChange<E> extends Move {

    protected Set<E> set;
    protected E element;
    protected boolean addToSet; // false = remove
    protected boolean noChange; // if move did not change anything

    public SetChange(Set<E> set, E element, boolean addToSet) {

        this.set = set;
        this.element = element;
        this.addToSet = addToSet;
        noChange = !(set.contains(element) ^ addToSet); // xor
        
        MoveSet.add(this);
    }

    @Override
    public boolean execute() {

        if (noChange) return true;
            
        if (addToSet)
            set.add(element);
        else
            set.remove(element);
        
        return true;
    }

    @Override
    public boolean undo() {

        if (noChange) return true;

        if (addToSet)
            set.remove(element);
        else
            set.add(element);

        return true;
    }
    
    public String toString() {
        StringBuffer s = new StringBuffer("Set Change: ");
        if (noChange) 
            s.append("No Change for element "+element);
        else
            if (addToSet) 
                s.append("Added element "+element);
            else
                s.append("Removed element "+element);
        return s.toString();
    }

}
