package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;


/**
 * @author Erik Vos
 * 
 * Rails 2.0: updated equals methods, updated toString method
 */
public abstract class StartItemAction extends PossibleAction {

    /* Server-provided fields */
    transient protected StartItem startItem;
    protected String startItemName;
    protected int itemIndex;

    public static final long serialVersionUID = 1L;

    /**
     * 
     */
    public StartItemAction(StartItem startItem) {

        super();
        this.startItem = startItem;
        this.startItemName = startItem.getName();
        this.itemIndex = startItem.getIndex();
    }

    /**
     * @return Returns the startItem.
     */
    public StartItem getStartItem() {
        return startItem;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public int getStatus() {
        return startItem.getStatus();
    }
    
    @Override 
    public boolean equalsAsOption(PossibleAction pa) {
        //  super checks both class identity and super class attributes
        if (!super.equalsAsOption(pa)) return false; 

        // check further attributes
        StartItemAction action = (StartItemAction)pa; 
        return Objects.equal(this.startItem, action.startItem);
    }
    
    @Override
    public boolean equalsAsAction (PossibleAction pa) {
        // no further test compared to option
        return this.equalsAsOption(pa);
    }

    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                .addToString("StartItem", startItem)
                .toString()
        ;
    }
    
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        startItem = StartItem.getByName(startItemName);

    }
}
