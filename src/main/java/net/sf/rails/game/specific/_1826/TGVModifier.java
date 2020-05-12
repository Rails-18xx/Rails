package net.sf.rails.game.specific._1826;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.algorithms.NetworkTrain;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.common.LocalText;


/**
 * TGV is a train that runs on independent track (defined in Game 1826)
 * implementation allows several tgv trains
 * @author freystef
 */
public class TGVModifier implements RevenueDynamicModifier {

    final private String TGV_NAME = "TGV";

    private int nbTGV = 0; // store the number of tgv

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {

        // separate trains into tgv and others
        List<NetworkTrain> allTrains = revenueAdapter.getTrains();
        List<NetworkTrain> tgvTrains = new ArrayList<NetworkTrain>();
        List<NetworkTrain> otherTrains = new ArrayList<NetworkTrain>();
        for (NetworkTrain train:allTrains) {
            // checks name of train
            if (train.getTrainName().equals(TGV_NAME)) {
                tgvTrains.add(train);
            } else {
                otherTrains.add(train);
            }
        }

        // change list that tgv trains are the first ones, if there are tgvs ...
        nbTGV = tgvTrains.size();
        if (nbTGV != 0) {
            allTrains.clear();
            allTrains.addAll(tgvTrains);
            allTrains.addAll(otherTrains);
            return true;
        } else { // ... otherwise deactivate modifier
            return false;
        }
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        // nothing to do here
        return 0;
    }

    @Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        // nothing to do here
        return 0;
    }

    @Override
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // nothing to do
    }

    public boolean providesOwnCalculateRevenue() {
        return true;
    }

    public int calculateRevenue(RevenueAdapter revenueAdapter) {
        // tgv run separately (see prepare modifier above)
        int value = 0;
        value = revenueAdapter.calculateRevenue(0, nbTGV-1);
        // add the other trains
        value += revenueAdapter.calculateRevenue(nbTGV, revenueAdapter.getTrains().size()-1);
        return value;
    }

    @Override
    public String prettyPrint(RevenueAdapter adapter) {
        return LocalText.getText("TGVModifier");
    }


}
