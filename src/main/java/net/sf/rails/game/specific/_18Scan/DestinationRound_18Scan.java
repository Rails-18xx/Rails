package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import rails.game.action.LayBaseToken;
import rails.game.action.NullAction;
import rails.game.action.SetDividend;

import static net.sf.rails.game.GameDef.OrStep.FINAL;

public class DestinationRound_18Scan extends OperatingRound {

    protected GameDef.OrStep[] steps = new GameDef.OrStep[]{
            GameDef.OrStep.INITIAL,
            GameDef.OrStep.LAY_TOKEN,
            GameDef.OrStep.CALC_REVENUE,
            FINAL
    };


    private PublicCompany destinationCompany;
    private MapHex destinationHex;
    private int destinationRunRevenue = 80; // Always added to the revenue
    private boolean forcedTokenLay = false;

    public DestinationRound_18Scan(GameManager parent, String id) {
        super(parent, id);
    }

    public void start(PublicCompany company) {

        setOperatingCompany(company);
        destinationCompany = company;
        destinationHex = company.getDestinationHex();

        setStep(GameDef.OrStep.INITIAL);

        ReportBuffer.add(this, LocalText.getText("StartDestinationRun", company));

        // Company may lay a token if it has one left, and if there is room or if the tile is yellow.
        if (company.hasBaseTokens() &&
                (destinationHex.hasTokenSlotsLeft() || destinationHex.getCurrentTile().getColourNumber() == 1)) {
            setStep(GameDef.OrStep.LAY_TOKEN);
            forcedTokenLay = !destinationHex.hasTokenSlotsLeft()
                    && destinationHex.getCurrentTile().getColourNumber() == 1; // Yellow
        } else if (company.canRunTrains()) {
            setStep(GameDef.OrStep.CALC_REVENUE);
        } else {
            setStep(GameDef.OrStep.FINAL);
        }
        //super.process(null);  // to get into the normal processing loop.
    }

    public boolean setPossibleActions() {

        if (getStep() == GameDef.OrStep.LAY_TOKEN) {
            possibleActions.add(new LayBaseToken(getRoot(), destinationHex, forcedTokenLay));
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));
        } else if (getStep() == GameDef.OrStep.CALC_REVENUE) {
            prepareRevenueAndDividendAction();
        }

        return true;
    }

    public boolean setRevenueAndDividend(SetDividend action) {
        destinationRunRevenue += action.getActualRevenue();
        ReportBuffer.add(this, LocalText.getText (
                "CompanySplits",
                operatingCompany.value().getId(),
                Bank.format(this, destinationRunRevenue)));
        splitRevenue(destinationRunRevenue);

        finishRound();
        return true;
    }

    protected void finishRound() {
        // Inform GameManager
        gameManager.nextRound(this);
    }
}