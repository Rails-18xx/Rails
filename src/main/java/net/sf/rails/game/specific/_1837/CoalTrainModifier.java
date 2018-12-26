package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.algorithms.NetworkTrain;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.common.LocalText;


/**
 * Coal Train Modifier based on TGV Modifier by Stefan Frey
 * implementation allows several Coal trains
 * @author MBrumm 
 */
public class CoalTrainModifier implements RevenueDynamicModifier {

        private int nbCOAL = 0; // store the number of Coal Trains
    
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        
        // separate trains into coal trains and others
        List<NetworkTrain> allTrains = revenueAdapter.getTrains();
        List<NetworkTrain> coalTrains = new ArrayList<NetworkTrain>();
        List<NetworkTrain> otherTrains = new ArrayList<NetworkTrain>();
        for (NetworkTrain train:allTrains) {
            // checks name of train
            if (train.getTrainName().contains("G") ) {
                coalTrains.add(train);
            } else {
                otherTrains.add(train);
            }
        }

        // change list that tgv trains are the first ones, if there are tgvs ...
        nbCOAL = coalTrains.size();
        if (nbCOAL != 0) {
            allTrains.clear();
            allTrains.addAll(coalTrains);
            allTrains.addAll(otherTrains);
            return true;
        } else { // ... otherwise deactivate modifier
            return false;
        }
    }

    public int predictionValue(List<RevenueTrainRun> runs) {
        // nothing to do here
        return 0;
    }

    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        // nothing to do here
        return 0;
    }

    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // nothing to do 
    }

    public boolean providesOwnCalculateRevenue() {
        return true;
    }

    public int calculateRevenue(RevenueAdapter revenueAdapter) {
        int value = 0;
        value = revenueAdapter.calculateRevenue(0, revenueAdapter.getTrains().size()-1);
        return value;
    }

    public String prettyPrint(RevenueAdapter adapter) {
        return LocalText.getText("CoalTrainModifier");
    }


}
