/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/StartItemAction.java,v 1.3 2008/06/04 19:00:29 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.StartItem;

/**
 * @author Erik Vos
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

    /** @deprecated */
    @Deprecated
    public int getStatus() {
        // if (startItem == null) return 0;//BAD
        return startItem.getStatus();
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        startItem = StartItem.getByName(startItemName);

    }
}
