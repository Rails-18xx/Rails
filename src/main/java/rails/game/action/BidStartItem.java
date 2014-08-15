package rails.game.action;

import com.google.common.base.Objects;

import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class BidStartItem extends StartItemAction {

    /* Server-provided fields */
    private int minimumBid;
    private int bidIncrement;
    private boolean selected;
    private boolean selectForAuction;

    /* Client-provided fields */
    private int actualBid = 0;

    public static final long serialVersionUID = 1L;

    /**
     * 
     */
    public BidStartItem(StartItem startItem, int minimumBid, int bidIncrement,
            boolean selected, boolean selectForAuction) {

        super(startItem);
        this.minimumBid = minimumBid;
        this.bidIncrement = bidIncrement;
        this.selected = selected;
        this.selectForAuction = selectForAuction;

    }

    public BidStartItem(StartItem startItem, int minimumBid, int bidIncrement,
            boolean selected) {

        this(startItem, minimumBid, bidIncrement, selected, false);

    }

    public int getMinimumBid() {
        return minimumBid;
    }

    public int getBidIncrement() {
        return bidIncrement;
    }

    public int getActualBid() {
        return actualBid;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isSelectForAuction() {
        return selectForAuction;
    }

    public void setActualBid(int actualBid) {
        this.actualBid = actualBid;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        BidStartItem action = (BidStartItem)pa; 
        boolean options = 
                Objects.equal(this.minimumBid, action.minimumBid)
                && Objects.equal(this.bidIncrement, action.bidIncrement)
                && Objects.equal(this.selected, action.selected)
                && Objects.equal(this.selectForAuction, action.selectForAuction)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.actualBid, action.actualBid)
        ;
    }

   @Override
   public String toString() {
       return super.toString() + 
               RailsObjects.stringHelper(this)
                   .addToString("minimumBid", minimumBid)
                   .addToString("bidIncrement", bidIncrement)
                   .addToString("selected", selected)
                   .addToString("selectForAuction", selectForAuction)
                   .addToStringOnlyActed("actualBid", actualBid)
                   .toString()
       ;
   }

}
