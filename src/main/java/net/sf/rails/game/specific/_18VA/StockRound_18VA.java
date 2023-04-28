package net.sf.rails.game.specific._18VA;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.state.Currency;
import rails.game.action.BuyCertificate;

public class StockRound_18VA extends StockRound {

    public StockRound_18VA(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    public boolean buyShares(String playerName, BuyCertificate action) {

        boolean result = super.buyShares (playerName, action);

        /* When the 6th share of a 10-share company is bought,
         * all remaining shares go to the Pool, and the company
         * is fully capitalised.
         *
         * Note: as this is part of a one-time player action,
         * and companies cannot trade their own shares,
         * there is no danger that this buy follow-up will be repeated.
         */
        PublicCompany company = action.getCompany();
        if (company.getShareUnit() == 10 && company.getPortfolioModel().getShares(company) == 4) {
            for (PublicCertificate cert : company.getPortfolioModel().getCertificates(company)) {
                cert.moveTo(pool);
            }
            int cash = 4 * company.getMarketPrice();
            String cashText = Currency.fromBank(cash, company);
            ReportBuffer.add(this, LocalText.getText("SELL_SHARES_LOG",
                    company,
                    4,
                    company.getShareUnit(),
                    (4 * company.getShareUnit()),
                    company,
                    cashText));

        }
        return result;
    }
}
