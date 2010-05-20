package rails.game.specific._18AL;

import rails.algorithms.NetworkTrain;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.game.TrainI;

public class NamedTrainRevenueModifier implements RevenueStaticModifier {

    public void modifyCalculator(RevenueAdapter revenueAdapter) {
        
        // 1. check all Trains for name Tokens
        for (NetworkTrain networkTrain:revenueAdapter.getTrains()) {
            TrainI train = networkTrain.getRailsTrain();
            if (!(train instanceof NameableTrain)) continue;
            NamedTrainToken token = ((NameableTrain)train).getNameToken();
            if (token == null) continue;
            // 2. define revenue bonus
            RevenueBonus bonus = new RevenueBonus(token.getValue(), token.getName());
            bonus.addTrain(train);
            for (NetworkVertex vertex:NetworkVertex.getVerticesByHexes(revenueAdapter.getVertices(), token.getHexesToPass())) {
                if (!vertex.isStation()) continue;
                bonus.addVertex(vertex);
            }
            revenueAdapter.addRevenueBonus(bonus);
        }
    }

}
