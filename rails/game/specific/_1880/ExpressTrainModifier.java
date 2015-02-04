package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rails.algorithms.NetworkTrain;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueDynamicModifier;
import rails.algorithms.RevenueTrainRun;
import rails.common.LocalText;

    /**
     * @author Martin Brumm - 2015-01-08
     *
     */
    public class ExpressTrainModifier implements RevenueDynamicModifier {

        private boolean hasExpress;

        public boolean prepareModifier(RevenueAdapter revenueAdapter) {
            // 1. check if there is a Express Train in the train set
            hasExpress = false;
            List<NetworkTrain> trains = revenueAdapter.getTrains();
            for (NetworkTrain train:trains) {
                if ((train.getRailsTrainType() != null) 
                        && ((train.getRailsTrainType().getCertificateType().getName().equals("6E")) 
                        ||(train.getRailsTrainType().getCertificateType().getName().equals("8E")))) {
                    hasExpress = true;
                    // We have an express Train
                    break;
                }
            }
            return hasExpress;
        }


        public int predictionValue() {
            return 0;
        }

        // TODO: Rails 2.0 rewrite this by using Guava
        private List<NetworkVertex> extractExpressRun(RevenueTrainRun run, int length) {
            
            // check for valid run first
            if (!run.hasAValidRun()) return new ArrayList<NetworkVertex>();
            
            NetworkVertex baseVertex = run.getFirstVertex();
   
            // create a copy of runlist, Collections.sorts has side effects
            List<NetworkVertex> nextVertices = new ArrayList<NetworkVertex>(
                    run.getRunVertices().subList(1, run.getRunVertices().size())); 
            
            
            Collections.sort(nextVertices); // Sort the Vertices by Value
            List<NetworkVertex> expressVertices = nextVertices.subList(0, Math.min(nextVertices.size(),length-1));
            expressVertices.add(0, baseVertex);
            
            return expressVertices;
        }
        
        
        public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
            int value = 0;
            //Find out which Express Train is involved
            for (RevenueTrainRun run:runs) {

                if ((run.getTrain().getRailsTrainType() != null) 
                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("6E"))) {
                    /*  We Found a run with an express Train, now we have to make sure that the result gets trimmed
                         to the maximum allowed stations, cause so far its a Diesel Train !
                     */
                    int expressRunValue = NetworkVertex.sum(extractExpressRun(run, 6));
                    value += expressRunValue - run.getRunValue();

                }
                if ((run.getTrain().getRailsTrainType() != null) 
                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("8E"))) {
                    // We Found a run with an express Train, now we have to make sure that the result gets trimmed
                    // to the maximum allowed stations, cause so far its a Diesel Train !
                    int expressRunValue = NetworkVertex.sum(extractExpressRun(run, 8));
                    value += expressRunValue - run.getRunValue();
                }
            }
            return value;
        }

        // this does not work yet, requires adjustments in underlying revenue code
        // TODO: Rails 2.0
//        private void adjustExpressRun(RevenueTrainRun run, List<NetworkVertex> expressVertices) {
//            for (NetworkVertex vertex:new ArrayList<NetworkVertex>(run.getRunVertices())) {
//                if (!expressVertices.contains(vertex)) {
//                   run.getRunVertices().remove(vertex);
//                }
//            }
//        }

        public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
            // this does not work yet, requires adjustments in underlying revenue code
            // TODO: Rails 2.0
            
//            //Find out which Express Train is involved
//            for (RevenueTrainRun run:optimalRuns) {
//                
//                if ((run.getTrain().getRailsTrainType() != null) 
//                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("6E"))) {
//                    /*  We Found a run with an express Train, now we have to make sure that the result gets trimmed
//                         to the maximum allowed stations, cause so far its a Diesel Train !
//                     */
//                    adjustExpressRun(run, extractExpressRun(run, 6));
//                }
//                if ((run.getTrain().getRailsTrainType() != null) 
//                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("8E"))) {
//                    // We Found a run with an express Train, now we have to make sure that the result gets trimmed
//                    // to the maximum allowed stations, cause so far its a Diesel Train !
//                    adjustExpressRun(run, extractExpressRun(run, 8));
//                }
//            }
            
        }

        public boolean providesOwnCalculateRevenue() {
            return false;
        }

        public int calculateRevenue(RevenueAdapter revenueAdpater) {
            return 0;
        }

        public String prettyPrint(RevenueAdapter revenueAdapter) {
            return LocalText.getText("1880ExpressAdjustment", evaluationValue(revenueAdapter.getOptimalRun(), true));
        }

 
    }
