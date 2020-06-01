package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.SetDividend;

public class OperatingRound_18Scan extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_18Scan.class);

    public OperatingRound_18Scan(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected void executeTrainlessRevenue (GameDef.OrStep step) {
        // Minors always pay out something.
        PublicCompany company = operatingCompany.value();
        if (step == GameDef.OrStep.CALC_REVENUE && company.isOfType("Minor")
                && !company.canRunTrains()) {
            int amount = 10;
            String report = LocalText.getText("NoTrainsButBankPaysAnyway",
                    company.getId(),
                    Bank.format(this, amount),
                    company.getPresident().getId());
            log.debug("OR skips {}: Cannot run trains but still pays {}", step, amount);
            SetDividend action = new SetDividend(getRoot(), 0, false,
                    new int[]{SetDividend.PAYOUT});
            action.setRevenueAllocation(SetDividend.PAYOUT);
            action.setActualRevenue(amount);
            executeSetRevenueAndDividend(action, report);
        }
    }
 }
