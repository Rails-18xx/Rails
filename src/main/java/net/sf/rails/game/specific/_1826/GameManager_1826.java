package net.sf.rails.game.specific._1826;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.IntegerState;

public class GameManager_1826 extends GameManager {

    private IntegerState eTrainsBought = IntegerState.create (this, "eTrainsBought", 0);
    private BooleanState tgvTrainBought = new BooleanState (this, "tgvBought", false);

    public GameManager_1826(RailsRoot parent, String id) {

        super(parent, id);
    }

    public int getETrainsBought() {
        return eTrainsBought.value();
    }

    public void setETrainsBought(int eTrainsBought) {
        this.eTrainsBought.set(eTrainsBought);
    }

    public void addETrainsBought () {
        setETrainsBought(getETrainsBought()+1);
    }

    public boolean getTgvTrainBought() {
        return tgvTrainBought.value();
    }

    public void setTgvTrainBought(boolean tgvTrainBought) {
        this.tgvTrainBought.set(tgvTrainBought);
    }
}
