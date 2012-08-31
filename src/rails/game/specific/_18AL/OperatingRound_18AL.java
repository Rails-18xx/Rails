package rails.game.specific._18AL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import rails.common.LocalText;
import rails.game.*;
import rails.game.action.LayBaseToken;
import rails.game.action.PossibleAction;

public class OperatingRound_18AL extends OperatingRound {

    /**
     * Constructed via Configure
     */
    public OperatingRound_18AL (GameManager parent, String id) {
        super (parent, id);
    }

    @Override
    protected void setGameSpecificPossibleActions() {
        // if optimized no need to assign
        if (getGameOption("18ALOptimizeNamedTrains").equalsIgnoreCase("yes")) return;
        
        for (NameTrains stl : getSpecialProperties(NameTrains.class)) {
            Set<Train> trains =
                    operatingCompany.value().getPortfolioModel().getTrainList();
            if (trains != null && !trains.isEmpty()) {
                possibleActions.add(new AssignNamedTrains(stl, trains));
            }

        }
    }

    @Override
    public boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof AssignNamedTrains) {

            AssignNamedTrains namingAction = (AssignNamedTrains) action;
            List<NameableTrain> trains = namingAction.getNameableTrains();
            List<NameableTrain> newTrainsPerToken =
                    namingAction.getPostTrainPerToken();
            List<NamedTrainToken> tokens = namingAction.getTokens();

            List<NamedTrainToken> newTokenPerTrain =
                    new ArrayList<NamedTrainToken>(trains.size());

            NameableTrain newTrain;
            NamedTrainToken oldToken, newToken;

            for (int i = 0; i < trains.size(); i++) {
                newTokenPerTrain.add(null);
            }
            for (int i = 0; i < tokens.size(); i++) {
                newTrain = newTrainsPerToken.get(i);
                if (newTrain != null)
                    newTokenPerTrain.set(trains.indexOf(newTrain),
                            tokens.get(i));
            }

            getRoot().getChangeStack().newChangeSet(action);

            for (int i = 0; i < trains.size(); i++) {
                oldToken = trains.get(i).getNameToken();
                newToken = newTokenPerTrain.get(i);
                if (oldToken != newToken) {
                    trains.get(i).setNameToken(newToken);
                    if (newToken != null) {
                        ReportBuffer.add(LocalText.getText("NamesTrain",
                                operatingCompany.value().getId(),
                                trains.get(i).getId(),
                                newToken.getLongName() ));
                    }
                }
            }
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean layBaseToken(LayBaseToken action) {
        if (super.layBaseToken(action)) {
            MapHex hex = action.getChosenHex();
            if (hex == operatingCompany.value().getDestinationHex()) {
                int payout = 100;
                String payoutText = Currency.fromBank(payout, operatingCompany.value());
                ReportBuffer.add(LocalText.getText("DestinationReachedByToken",
                        operatingCompany.value().getId(),
                        payoutText,
                        hex.getId() ));
            }
            return true;
        } else {
            return false;
        }

    }
}
