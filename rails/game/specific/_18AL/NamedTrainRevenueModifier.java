package rails.game.specific._18AL;

import java.util.ArrayList;
import java.util.List;

import rails.algorithms.NetworkTrain;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueDynamicModifier;
import rails.algorithms.RevenueStaticModifier;
import rails.algorithms.RevenueTrainRun;
import rails.game.ConfigurableComponentI;
import rails.game.ConfigurationException;
import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.TrainI;
import rails.util.Tag;

public class NamedTrainRevenueModifier implements RevenueStaticModifier, RevenueDynamicModifier, ConfigurableComponentI {

    private boolean dynamic;
    private List<RevenueBonus> bonuses;
    private int bonusMaximum;
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        // do nothing
    }

    public void finishConfiguration(GameManagerI parent)
            throws ConfigurationException {
        dynamic = parent.getGameOption("18ALOptimizeNamedTrains").equalsIgnoreCase("yes");
    }
    
    private RevenueBonus defineBonus(RevenueAdapter revenueAdapter, NamedTrainToken token, boolean useLongname) {
        RevenueBonus bonus;
        if (useLongname) {
            bonus = new RevenueBonus(token.getValue(), token.getLongName()); 
        } else {
            bonus = new RevenueBonus(token.getValue(), token.getName());
        }

        for (MapHex hex:token.getHexesToPass()) {
            boolean stationWasFound = false;
            for (NetworkVertex vertex:NetworkVertex.getVerticesByHex(revenueAdapter.getVertices(), hex)) {
                if (!vertex.isStation()) continue;
                bonus.addVertex(vertex);
                stationWasFound = true;
            }
            // if for one vertex no station is found then the bonus is set null
            if (!stationWasFound) {
                bonus = null;
                break;
            }
        }
        return bonus;
    }
    
    
    public void modifyCalculator(RevenueAdapter revenueAdapter) {
        // static modifier
        if (dynamic) return;
        
        // 1. check all Trains for name Tokens
        for (NetworkTrain networkTrain:revenueAdapter.getTrains()) {
            TrainI train = networkTrain.getRailsTrain();
            if (!(train instanceof NameableTrain)) continue;
            NamedTrainToken token = ((NameableTrain)train).getNameToken();
            if (token == null) continue;
            // 2. define revenue bonus
            RevenueBonus bonus = defineBonus(revenueAdapter, token, false);
            if (bonus == null) continue;
            bonus.addTrain(train);
            revenueAdapter.addRevenueBonus(bonus);
        }
    }

    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        // dynamic modifier
        if (!dynamic) return false;
        
        // 1. check if name trains special properties is available
        List<NameTrains> sp = revenueAdapter.getCompany().getPortfolio().getSpecialProperties(NameTrains.class, false);
        if (sp.isEmpty()) return false;
        
        // 2. prepare by defining the vertices
        bonuses = new ArrayList<RevenueBonus>();
        bonusMaximum = 0;
        // 3. there is only one special property in 18AL, thus get tokens from it
        for (NamedTrainToken token:sp.get(0).getTokens()) {
            RevenueBonus bonus = defineBonus(revenueAdapter, token, true);
            if (bonus == null) continue;
            bonuses.add(bonus);
            bonusMaximum += token.getValue();
        }
        return true;
    }

    public int predictionValue() {
        return bonusMaximum;
    }

    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        int bonusValue = 0;
        // due to the geography (off-map areas!) each train can only score one bonus
        for (RevenueBonus bonus:bonuses) {
            for (RevenueTrainRun run:runs) {
                if (run.getUniqueVertices().containsAll(bonus.getVertices())) {
                    bonusValue += bonus.getValue();
                    continue; // each bonus can only be scored once
                }
            }
        }
        return bonusValue;
    }

    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // do nothing here (all is done by changing the evaluation value)
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        List<RevenueTrainRun> runs = revenueAdapter.getOptimalRun();
        StringBuffer prettyPrint = new StringBuffer();
        for (RevenueBonus bonus:bonuses) {
            for (RevenueTrainRun run:runs) {
                if (run.getUniqueVertices().containsAll(bonus.getVertices())) {
                    prettyPrint.append(bonus.getName() + " = " + bonus.getValue() + "\n");
                    continue; // each bonus can only be scored once
                }
            }
        }
        return prettyPrint.toString();
    }



}
