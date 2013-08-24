/**
 * 
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.GameOption;
import rails.game.Bank;
import rails.game.GameDef;
import rails.game.GameManagerI;
import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.RoundI;
import rails.game.ShareSellingRound;
import rails.game.StockSpaceI;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;

/**
 * @author Martin
 *
 */
public class ShareSellingRound_1880 extends ShareSellingRound {

    /**
     * @param gameManager
     * @param parentRound
     */
    public ShareSellingRound_1880(GameManagerI gameManager, RoundI parentRound) {
        super(gameManager, parentRound);
        // TODO Auto-generated constructor stub
    }

    
    @Override
    public boolean setPossibleActions() {
       
        possibleActions.clear();

        setSellableShares();
                   
        possibleActions.add(new NullAction(NullAction.DONE));
        
        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(currentPlayer.getName() + " may: " + pa.toString());
        }

        return true;
    }


    /* (non-Javadoc)
     * @see rails.game.StockRound#process(rails.game.action.PossibleAction)
     */
    @Override
    public boolean process(PossibleAction action) {
 
        currentPlayer = getCurrentPlayer();

        if (action instanceof NullAction) {

             currentPlayer.addCash(-cashToRaise.intValue());
             gameManager.finishShareSellingRound();
             return true;
        } else {
        return super.process(action);
        }
    }


    /* (non-Javadoc)
     * @see rails.game.ShareSellingRound#sellShares(rails.game.action.SellShares)
     */
    @Override
    public boolean sellShares(SellShares action) {
        Portfolio portfolio = currentPlayer.getPortfolio();
        String playerName = currentPlayer.getName();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompanyI company =
            companyManager.getPublicCompany(action.getCompanyName());
        PublicCertificateI cert = null;
        PublicCertificateI presCert = null;
        List<PublicCertificateI> certsToSell =
            new ArrayList<PublicCertificateI>();
        Player dumpedPlayer = null;
        int presSharesToSell = 0;
        int numberToSell = action.getNumber();
        int shareUnits = action.getShareUnits();
        int currentIndex = getCurrentPlayerIndex();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            if (numberToSell <= 0) {
                errMsg = LocalText.getText("NoSellZero");
                break;
            }

            // Check company
            if (company == null) {
                errMsg = LocalText.getText("NoCompany");
                break;
            }

            // May player sell this company
            if (!mayPlayerSellShareOfCompany(company)) {
                errMsg = LocalText.getText("SaleNotAllowed", companyName);
                break;
            }

            // The player must have the share(s)
            if (portfolio.getShare(company) < numberToSell) {
                errMsg = LocalText.getText("NoShareOwned");
                break;
            }

            // The pool may not get over its limit.
            if (pool.getShare(company) + numberToSell * company.getShareUnit()
                    > getGameParameterAsInt(GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            // Find the certificates to sell
            Iterator<PublicCertificateI> it =
                portfolio.getCertificatesPerCompany(companyName).iterator();
            while (numberToSell > 0 && it.hasNext()) {
                cert = it.next();
                if (cert.isPresidentShare()) {
                    // Remember the president's certificate in case we need it
                    if (cert.isPresidentShare()) presCert = cert;
                    continue;
                } else if (shareUnits != cert.getShares()) {
                    // Wrong number of share units
                    continue;
                }
                // OK, we will sell this one
                certsToSell.add(cert);
                numberToSell--;
            }
            if (numberToSell == 0) presCert = null;

            if (numberToSell > 0 && presCert != null
                    && numberToSell <= presCert.getShares()) {
                // Not allowed to dump the company that needs the train
                if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed) {
                    errMsg =
                        LocalText.getText("CannotDumpTrainBuyingPresidency");
                    break;
                }
                // More to sell and we are President: see if we can dump it.
                Player otherPlayer;
                for (int i = currentIndex + 1; i < currentIndex
                + numberOfPlayers; i++) {
                    otherPlayer = gameManager.getPlayerByIndex(i);
                    if (otherPlayer.getPortfolio().getShare(company) >= presCert.getShare()) {
                        // Check if he has the right kind of share
                        if (numberToSell > 1
                                || otherPlayer.getPortfolio().ownsCertificates(
                                        company, 1, false) >= 1) {
                            // The poor sod.
                            dumpedPlayer = otherPlayer;
                            presSharesToSell = numberToSell;
                            numberToSell = 0;
                            break;
                        }
                    }
                }
            }
            // Check if we could sell them all
            if (numberToSell > 0) {
                if (presCert != null) {
                    errMsg = LocalText.getText("NoDumping");
                } else {
                    errMsg = LocalText.getText("NotEnoughShares");
                }
                break;
            }

            break;
        }

        int numberSold = action.getNumber();
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantSell",
                    playerName,
                    numberSold,
                    companyName,
                    errMsg ));
            return false;
        }

        // All seems OK, now do the selling.
        StockSpaceI sellPrice;
        int price;

        // Get the sell price (does not change within a turn)
        if (sellPrices.containsKey(companyName)
                && GameOption.convertValueToBoolean(getGameOption("SeparateSalesAtSamePrice"))) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            sellPrice = company.getCurrentSpace();
            price = sellPrice.getPrice();
            sellPrices.put(companyName, sellPrice);
        }
        int cashAmount = ((numberSold * price * shareUnits)-(numberSold * 5)); //Deduct 5 Yuan per Sharecertificate Sold...

        moveStack.start(true).linkToPreviousMoveSet();

        ReportBuffer.add(LocalText.getText("SELL_SHARES_LOG",
                playerName,
                numberSold,
                company.getShareUnit(),
                numberSold * company.getShareUnit(),
                companyName,
                Bank.format(cashAmount) ));

        boolean soldBefore = sellPrices.containsKey(companyName);

        pay (bank, currentPlayer, cashAmount);
        adjustSharePrice (company, numberSold, soldBefore);

        if (!company.isClosed()) {

            executeShareTransfer (company, certsToSell,
                    dumpedPlayer, presSharesToSell, action.getPresidentExchange());
        }

        cashToRaise.add(-numberSold * price);

        if (cashToRaise.intValue() <= 0) {
            gameManager.finishShareSellingRound();
        } else if (getSellableShares().isEmpty()) {
            gameManager.finishShareSellingRound();
        }

        return true;        // TODO Auto-generated method stub
    }
    
    
}
