package net.sf.rails.ui.swing.gamespecific._SOH;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.GameStatus;
import rails.game.action.BuyCertificate;
import rails.game.action.StartCompany;

import java.util.Arrays;
import java.util.List;

public class GameStatus_SOH extends GameStatus {

    /**
     * Setup a button for buying share(s) to start a new company, usually the President's share.
     * Extracted from actionPerformed() to allow overriding, as required for SOH,
     * where all shares to float a company must be bought as one StartCompany action.
     * @param buy A StartCompany action object
     * @param buyActions List of BuyCertificate actions
     * @param buyAmounts Price of BuyCertificate actions
     * @param options Text to display with each possible initial share price
     */
    @Override
    protected void setupStartCompany (StartCompany buy, List<BuyCertificate> buyActions,
                                      List<Integer> buyAmounts, List<String> options)  {
        int[] startPrices;
        PublicCompany company = buy.getCompany();
        startPrices = buy.getStartPrices();
        Arrays.sort(startPrices);
        for (int i = 0; i < startPrices.length; i++) {
            options.add(LocalText.getText("StartCompanyMultipleShares",
                    company.getId(),
                    gameUIManager.format(startPrices[i]),
                    buy.getMaximumNumber(),
                    gameUIManager.format(buy.getMaximumNumber() * startPrices[i]) ));
            buyActions.add(buy);
            buyAmounts.add(startPrices[i]);
        }
   }

}
