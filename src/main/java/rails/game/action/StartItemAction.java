package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;


/**
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
        super(null); // not defined by an activity yet
        this.startItem = startItem;
        this.startItemName = startItem.getId();
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
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        StartItemAction action = (StartItemAction)pa; 
        return Objects.equal(this.startItem, action.startItem)
                && Objects.equal(this.itemIndex, action.itemIndex)
        ;
        // no asAction attributes to be checked
    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                .addToString("startItem", startItem)
                .addToString("itemIndex", itemIndex)
                .toString()
        ;
    }
    
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();
        startItem = root.getCompanyManager().getStartItemById(startItemName);
    }
}
