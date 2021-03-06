package net.sf.rails.game.specific._18TN;

import net.sf.rails.algorithms.NetworkTrain;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.BooleanState;

import java.util.Collections;
import java.util.List;


public final class PublicCompany_18TN extends PublicCompany implements RevenueStaticModifier {

    private final BooleanState civilWar = new BooleanState(this, "civilWar");

    public PublicCompany_18TN(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {
        super.finishConfiguration(root);
        root.getRevenueManager().addStaticModifier(this);
    }

    public boolean isCivilWar() {
        return civilWar.value();
    }

    public void setCivilWar(boolean value) {
        civilWar.set(value);
    }

    /**
     * Don't move the space if the company has one train in the civil war
     * (the revenue amount must then be zero)
     */
    @Override
    public void withhold(int amount) {
        if (isCivilWar() && portfolio.getNumberOfTrains() == 1) return;
        getRoot().getStockMarket().withhold(this);
    }

    /**
     * Modify the revenue calculation for the civil war by removing the shortest train
     */
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {

        // check first if it is the company for the revenue calculation
        if (revenueAdapter.getCompany() != this) return false;

        // check if it is civil war, otherwise no effect
        if (!isCivilWar()) return false;

        List<NetworkTrain> trains = revenueAdapter.getTrains();
        if (trains.size() == 0) return false; // no train, no effect

        // sort trains in ascending order (by domination which is equal to length for TN)
        Collections.sort(trains);

        // and remove the first train (shortest)
        trains.remove(0);

        return true;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return LocalText.getText("CivilWarActive");
    }

}
