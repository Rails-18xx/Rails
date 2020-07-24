package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.LayBaseToken;
import rails.game.action.NullAction;
import rails.game.action.SetDividend;

import java.util.List;

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

    private static final Logger log = LoggerFactory.getLogger(DestinationRound_18Scan.class);

    public DestinationRound_18Scan(GameManager parent, String id) {
        super(parent, id);
    }

    public void start(PublicCompany company) {

        setOperatingCompany(company);
        destinationCompany = company;
        destinationHex = company.getDestinationHex();

        setStep(GameDef.OrStep.INITIAL);

        ReportBuffer.add(this, LocalText.getText("DestinationRoundStart", company));
        DisplayBuffer.add(this, LocalText.getText("DestinationRoundStart", company));

        // Company may lay a token if it has one left, and if there is room or if the tile is yellow.
        if (company.hasBaseTokens() &&
                (destinationHex.hasTokenSlotsLeft() || destinationHex.getCurrentTile().getColourNumber() == 1)) {
            setStep(GameDef.OrStep.LAY_TOKEN);
            forcedTokenLay = !destinationHex.hasTokenSlotsLeft()
                    && destinationHex.getCurrentTile().getColourNumber() == 1; // Yellow

        } else {
            // Even without trains, there will be revenue
            setStep(GameDef.OrStep.CALC_REVENUE);
        }
    }

    public boolean setPossibleActions() {

        if (getStep() == GameDef.OrStep.LAY_TOKEN) {
            setSpecialTokenLays();
            possibleActions.addAll(currentSpecialTokenLays);
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));
        } else if (getStep() == GameDef.OrStep.CALC_REVENUE) {
            prepareRevenueAndDividendAction();
            if (noMapMode) prepareNoMapActions();
        }

        return true;
    }

    @Override
    protected void setNormalTokenLays() {}

    @Override
    protected void setSpecialTokenLays() {
        /* Special-property tile lays */
        currentSpecialTokenLays.clear();

        // Check if the company still has tokens
        if (destinationCompany.getNumberOfFreeBaseTokens() == 0) return;

        for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
               // If this STL is location specific, check if there
                // isn't already a token of this company or if it is blocked
            List<MapHex> locations = stl.getLocations();
            if (locations != null && !locations.isEmpty()) {
                boolean canLay = false;
                for (MapHex location : locations) {
                    if (location.hasTokenOfCompany(destinationCompany)) {
                        continue;
                    }
                    for (Stop stop : location.getStops()) {
                        // TODO: Need to modify this
                        canLay = !location.isBlockedForTokenLays(destinationCompany, stop)
                                // An 18Scan special: lay aside the city if tile is yellow
                                || location.getCurrentTile().getColour() == TileColour.YELLOW;
                    }
                }
                if (!canLay) continue;
            }
            currentSpecialTokenLays.add(new LayBaseToken(getRoot(), stl));
         }
    }

    @Override
    public void executeSetRevenueAndDividend(SetDividend action) {
        int addedRevenue = destinationRunRevenue;
        destinationRunRevenue += action.getActualRevenue();
        String report = LocalText.getText (
                "DestinationRunRevenue",
                destinationCompany.getId(),
                Bank.format(this, action.getActualRevenue()),
                Bank.format(this, addedRevenue),
                Bank.format(this, destinationRunRevenue));
        ReportBuffer.add (this, report);
        report += "<br>" + LocalText.getText("DestinationRoundEnd", destinationCompany);
        DisplayBuffer.add (this, report);
        ReportBuffer.add(this, LocalText.getText (
                "CompanySplits",
                destinationCompany.getId(),
                Bank.format(this, destinationRunRevenue)));
        splitRevenue(destinationRunRevenue);
        ReportBuffer.add (this, LocalText.getText("DestinationRoundEnd", destinationCompany));

        finishRound();
    }

    protected void executeTrainlessRevenue (GameDef.OrStep step) {
        // Minors always pay out something.
        if (step == GameDef.OrStep.CALC_REVENUE && !destinationCompany.hasTrains()) {
            String report = LocalText.getText (
                    "DestinationRunRevenue",
                    destinationCompany.getId(),
                    Bank.format(this, 0),
                    Bank.format(this, destinationRunRevenue),
                    Bank.format(this, destinationRunRevenue));
            ReportBuffer.add (this, report);
            report += "<br>" + LocalText.getText("DestinationRoundEnd", destinationCompany);
            DisplayBuffer.add (this, report);
            log.debug("OR skips {}: Cannot run trains but still pays {}", step, destinationRunRevenue);

            SetDividend action = new SetDividend(getRoot(), 0, false,
                    new int[]{SetDividend.SPLIT});
            action.setRevenueAllocation(SetDividend.SPLIT);
            action.setActualRevenue(destinationRunRevenue);
            destinationCompany.setLastRevenue(destinationRunRevenue);
            destinationCompany.setLastRevenueAllocation(SetDividend.SPLIT);
            splitRevenue(destinationRunRevenue);
            ReportBuffer.add (this, LocalText.getText("DestinationRoundEnd", destinationCompany));

            finishRound();
        }
    }

    protected void finishRound() {
        // Inform GameManager
        gameManager.nextRound(this);
    }

    public String getRoundName() {
        return "Destination round for minor "+destinationCompany.getId();
    }
}