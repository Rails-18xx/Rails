/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Train.java,v 1.14 2009/10/10 15:25:49 evos Exp $ */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolderI;
import rails.game.move.ObjectMove;
import rails.game.state.BooleanState;

public class Train implements TrainI {

    protected TrainTypeI type;

    protected int majorStops;
    protected int minorStops;
    protected int cost;
    protected int cityScoreFactor;
    protected int townScoreFactor;
    protected int townCountIndicator;

    protected String uniqueId;
    protected static Map<String, TrainI> trainMap =
            new HashMap<String, TrainI>();

    protected Portfolio holder;
    protected BooleanState obsolete;

    protected static final Portfolio unavailable = Bank.getInstance().getUnavailable();
    protected static final Portfolio ipo = Bank.getInstance().getIpo();

    protected static Logger log =
            Logger.getLogger(Train.class.getPackage().getName());

    public Train() {}

    public void init(TrainTypeI type, int index) {

        this.type = type;
        this.majorStops = type.getMajorStops();
        this.minorStops = type.getMinorStops();
        this.cost = type.getCost();
        this.cityScoreFactor = type.getCityScoreFactor();
        this.townScoreFactor = type.getTownScoreFactor();
        this.townCountIndicator = type.getTownCountIndicator();

        uniqueId = type.getName() + "_" + index;
        trainMap.put(uniqueId, this);

        obsolete = new BooleanState(uniqueId, false);
    }

    public static TrainI getByUniqueId(String id) {
        return trainMap.get(id);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return cityScoreFactor;
    }

    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return cost;
    }

    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return majorStops;
    }

    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return minorStops;
    }

    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return townCountIndicator;
    }

    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return townScoreFactor;
    }

    /**
     * @return Returns the type.
     */
    public TrainTypeI getType() {
        return type;
    }

    public String getName() {
        return type.getName();
    }

    public Portfolio getHolder() {
        return holder;
    }

    public CashHolder getOwner() {
        return holder.getOwner();
    }

    public boolean isObsolete() {
        return obsolete.booleanValue();
    }

    /**
     * Move the train to another Portfolio.
     */
    public void setHolder(Portfolio newHolder) {
        holder = newHolder;
    }

    public void moveTo(MoveableHolderI to) {

        new ObjectMove(this, holder, to);
        
    }

    public void setRusted() {
        new ObjectMove(this, holder, Bank.getInstance().getScrapHeap());
    }

    public void setObsolete() {
        obsolete.set(true);
    }

    public boolean canBeExchanged() {
        return type.nextCanBeExchanged();
    }

    public String toDisplay() {
        return getName();
    }
    
}
