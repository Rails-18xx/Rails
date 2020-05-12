package net.sf.rails.game.specific._18EU;

import java.util.Collection;
import java.util.List;

import net.sf.rails.algorithms.NetworkTrain;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.common.LocalText;


public class PullmanRevenueModifier implements RevenueDynamicModifier {

    private boolean hasPullman;
    private int maxValue;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        // 1. check if there is a Pullman in the train set
        hasPullman = false;
        List<NetworkTrain> trains = revenueAdapter.getTrains();
        for (NetworkTrain train:trains) {
            if (train.getRailsTrainType() != null
                    && train.getRailsTrainType().getCertificateType().getId().equals("P")) {
                hasPullman = true;
                revenueAdapter.removeTrain(train); // remove from revenueAdapter
                break;
            }
        }
        if (!hasPullman) return false;
        // 2. find the maximum value of the vertices
        maxValue = maximumMajorValue(revenueAdapter.getVertices());
        return true;
    }

    @Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        return pullmanValue(runs);
    }

    private int pullmanValue(List<RevenueTrainRun> trainRuns) {
        int maximum = 0;
        for (RevenueTrainRun trainRun:trainRuns) {
            maximum = Math.max(maximum, maximumMajorValue(trainRun.getRunVertices()));
            if (maximum == maxValue) break;
        }
        return maximum;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        return maxValue;
    }

    public boolean providesOwnCalculateRevenue() {
        // does not
        return false;
    }

    public int calculateRevenue(RevenueAdapter revenueAdpater) {
        // zero does no change
        return 0;
    }
    @Override
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // do nothing here (all is done by changing the evaluation value)
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return LocalText.getText("Pullman") + " = " + pullmanValue(revenueAdapter.getOptimalRun());
    }

    private int maximumMajorValue(Collection<NetworkVertex> vertices) {
        int maximum = 0;
        for (NetworkVertex vertex:vertices) {
            if (!vertex.isMajor()) continue;
            maximum= Math.max(maximum, vertex.getValue());
        }
        return maximum;
    }

}
