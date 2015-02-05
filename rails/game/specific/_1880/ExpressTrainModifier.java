package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import rails.algorithms.NetworkTrain;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueDynamicModifier;
import rails.algorithms.RevenueTrainRun;

    public class ExpressTrainModifier implements RevenueDynamicModifier {
        
        protected static Logger log =
                Logger.getLogger(ExpressTrainModifier.class);

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
            
            NetworkVertex baseVertex = run.getBaseVertex();
   
            // create a sorted list of the run vertices
            List<NetworkVertex> otherVertices = new ArrayList<NetworkVertex>(run.getUniqueVertices()); 
            otherVertices.remove(baseVertex);
            Collections.sort(otherVertices);

            List<NetworkVertex> expressVertices = otherVertices.subList(0, Math.min(otherVertices.size(),length-1));
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
                    if (optimalRuns) log.debug("Express Long Run = " + run.getRunVertices());
                    List<NetworkVertex> expressRun = extractExpressRun(run, 6);
                    if (optimalRuns) log.debug("Express Best Run = " + expressRun);
                    int expressRunValue = NetworkVertex.sum(expressRun);
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

        public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
            // this does not work yet, requires adjustments in underlying revenue code
            // TODO: Rails 2.0
            
            //Find out which Express Train is involved
            for (RevenueTrainRun run:optimalRuns) {
                
                if ((run.getTrain().getRailsTrainType() != null) 
                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("6E"))) {
                    /*  We Found a run with an express Train, now we have to make sure that the result gets trimmed
                         to the maximum allowed stations, cause so far its a Diesel Train !
                     */
                    run.getRunVertices().retainAll(extractExpressRun(run, 6));
                }
                if ((run.getTrain().getRailsTrainType() != null) 
                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("8E"))) {
                    // We Found a run with an express Train, now we have to make sure that the result gets trimmed
                    // to the maximum allowed stations, cause so far its a Diesel Train !
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

        public String prettyPrint(RevenueAdapter revenueAdapter) {
            return null;
        }

 
    }
