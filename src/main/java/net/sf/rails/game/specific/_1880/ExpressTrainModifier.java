package net.sf.rails.game.specific._1880;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import net.sf.rails.algorithms.NetworkTrain;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;

    public class ExpressTrainModifier implements RevenueDynamicModifier {

        private static final String TRAIN_6E = "6E";
        private static final String TRAIN_8E = "8E";

        private static final Logger log = LoggerFactory.getLogger(ExpressTrainModifier.class);

        private boolean hasExpress;

        @Override
        public boolean prepareModifier(RevenueAdapter revenueAdapter) {
            // 1. check if there is a Express Train in the train set
            hasExpress = false;
            List<NetworkTrain> trains = revenueAdapter.getTrains();
            for (NetworkTrain train:trains) {
                if (TRAIN_6E.equals(train.getTrainName()) || TRAIN_8E.equals(train.getTrainName())) {
                    hasExpress = true;
                    break;
                }
            }
            return hasExpress;
        }


        private List<NetworkVertex> extractExpressRun(RevenueTrainRun run, int length) {

            // check for valid run first
            if (!run.hasAValidRun()) return new ArrayList<NetworkVertex>();

            // create a sorted list of the run vertices
            List<NetworkVertex> sortedVertices =
                    Ordering.from(new NetworkVertex.ValueOrder()).immutableSortedCopy(run.getUniqueVertices());

            ImmutableList.Builder<NetworkVertex> expressVertices = ImmutableList.builder();
            NetworkVertex baseVertex = run.getBaseVertex();
            expressVertices.add(baseVertex);

            int inRunNumber = 1;
            for (NetworkVertex vertex:sortedVertices) {
                if (vertex != baseVertex) {
                    if (!vertex.isStation()) {
                        // keep ferry malus vertices
                        expressVertices.add(vertex);
                    } else if (inRunNumber < length) {
                        // add vertices until length is reached
                        expressVertices.add(vertex);
                        inRunNumber ++;
                    }
                }
            }

            return expressVertices.build();
        }

        private int valueChange(List<RevenueTrainRun> runs, boolean optimalRuns) {
            int value = 0;
            //Find out which Express Train is involved
            for (RevenueTrainRun run:runs) {
                if (TRAIN_6E.equals(run.getTrain().getTrainName())) {
                    if (optimalRuns) log.debug("Express Long Run = {}", run.getRunVertices());
                    List<NetworkVertex> expressRun = extractExpressRun(run, 6);
                    if (optimalRuns) log.debug("Express Best Run = {}", expressRun);
                    int expressRunValue = run.getRunValueForVertices(expressRun);
                    value += expressRunValue - run.getRunValue();
                } else if (TRAIN_8E.equals(run.getTrain().getTrainName())) {
                    if (optimalRuns) log.debug("Express Long Run = {}", run.getRunVertices());
                    List<NetworkVertex> expressRun = extractExpressRun(run, 8);
                    if (optimalRuns) log.debug("Express Best Run = {}", expressRun);
                    int expressRunValue = run.getRunValueForVertices(expressRun);
                    value += expressRunValue - run.getRunValue();
                }
            }
            return value;
        }

        @Override
        public int predictionValue(List<RevenueTrainRun> runs) {
            return valueChange(runs, false);
        }

        @Override
        public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
            return valueChange(runs, optimalRuns);
        }

        @Override
        public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
            //Find out which Express Train is involved
            for (RevenueTrainRun run:optimalRuns) {
                if (TRAIN_6E.equals(run.getTrain().getTrainName())) {
                    run.getRunVertices().retainAll(extractExpressRun(run, 6));
                }
                if (TRAIN_8E.equals(run.getTrain().getTrainName())) {
                    run.getRunVertices().retainAll(extractExpressRun(run, 8));
                }
            }

        }

        public boolean providesOwnCalculateRevenue() {
            return false;
        }

        public int calculateRevenue(RevenueAdapter revenueAdpater) {
            return 0;
        }

        @Override
        public String prettyPrint(RevenueAdapter revenueAdapter) {
            return null;
        }


    }
