package rails.game.specific._1880;


import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import rails.algorithms.NetworkTrain;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueDynamicModifier;
import rails.algorithms.RevenueTrainRun;

    /**
     * @author Martin Brumm - 2015-01-08
     *
     */
    public class ExpressTrainModifier implements RevenueDynamicModifier {

        
        protected static Logger log =
                Logger.getLogger(ExpressTrainModifier.class.getPackage().getName());

        private boolean hasExpress;

        /**
         * 
         */
        public ExpressTrainModifier() {
            // TODO Auto-generated constructor stub
        }

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

        public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {

            int evaluationValue = 0;
            if (hasExpress) {
                int value = 0; 
                //Find out which Express Train is involved
                 for (RevenueTrainRun run:runs) {
                     
                      if ((run.getTrain().getRailsTrainType() != null) 
                        && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("6E"))) {
                        /*  We Found a run with an express Train, now we have to make sure that the result gets trimmed
                         to the maximum allowed stations, cause so far its a Diesel Train !
                        */
                         List<NetworkVertex> preExpressVertices = run.getRunVertices();
                        Collections.sort(preExpressVertices); // Sort the Vertices by Value
                        // Now we need to throw out all Values beyond the first 6 and reset the run vertices...
                        int i=0;
                        for (NetworkVertex preExpressVertex:preExpressVertices) {
                            i=i+1;
                            if (i <=6) {
                                value += preExpressVertex.getValue(); //Value of the 6E Run
                            }
                        }
                     }
                     if ((run.getTrain().getRailsTrainType() != null) 
                             && (run.getTrain().getRailsTrainType().getCertificateType().getName().equals("8E"))) {
                              // We Found a run with an express Train, now we have to make sure that the result gets trimmed
                              // to the maximum allowed stations, cause so far its a Diesel Train !
                             List<NetworkVertex> preExpressVertices =  run.getRunVertices();
                             Collections.sort(preExpressVertices); // Sort the Vertices by Value
                             // Now we need to throw out all Values beyond the first 6 and reset the run vertices...
                             int i=0;
                             for (NetworkVertex preExpressVertex:preExpressVertices) {
                                 i=i+1;
                                 if (i <=8) {
                                     value = value + preExpressVertex.getValue(); //Value of the 8E Run combined with 6E
                                 }
                             }
                             
                          }
                     if (value !=0) {
                         evaluationValue -= value;
                     }
                 }
                 log.info("EvaluationValue:"+ evaluationValue);
                 return evaluationValue;
            }
                        
            return 0;
        }

        public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {

            
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
