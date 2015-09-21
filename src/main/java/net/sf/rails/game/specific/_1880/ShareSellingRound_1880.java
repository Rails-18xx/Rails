/**
 * 
 */
package net.sf.rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.GameDef;
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
    public ShareSellingRound_1880(GameManager parent, String id) {
        super(parent, id);
    }

    
    @Override
    public boolean setPossibleActions() {
       
        possibleActions.clear();

        setSellableShares();
                   
        possibleActions.add(new NullAction(NullAction.Mode.DONE));
        
        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(currentPlayer.getId() + " may: " + pa.toString());
        }

        return true;
    }


    /* (non-Javadoc)
     * @see rails.game.StockRound#process(rails.game.action.PossibleAction)
     */
    @Override
    public boolean process(PossibleAction action) {
        currentPlayer = playerManager.getCurrentPlayer();
        
        if (action instanceof NullAction) {
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
        PortfolioModel portfolio = currentPlayer.getPortfolioModel();
        String playerName = currentPlayer.getId();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompany company =
            companyManager.getPublicCompany(action.getCompanyName());
        PublicCertificate cert = null;
        PublicCertificate presCert = null;
        List<PublicCertificate> certsToSell =
                new ArrayList<PublicCertificate>();
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
                    > GameDef.getGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            // Find the certificates to sell
            Iterator<PublicCertificate> it =
                    portfolio.getCertificates(company).iterator();
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
                Player player = playerManager.getCurrentPlayer();
                for (int i = currentIndex + 1; i < currentIndex
                + numberOfPlayers; i++) {
                    otherPlayer = playerManager.getNextPlayerAfter(player);
                    if (otherPlayer.getPortfolioModel().getShare(company) >= presCert.getShare()) {
                        // Check if he has the right kind of share
                        if (numberToSell > 1
                                || otherPlayer.getPortfolioModel().ownsCertificates(
                                        company, 1, false) >= 1) {
                            // The poor sod.
                            dumpedPlayer = otherPlayer;
                            presSharesToSell = numberToSell;
                            numberToSell = 0;
                            break;
                        }
                    }
                    player = otherPlayer;
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
            DisplayBuffer.add(this, LocalText.getText("CantSell",
                    playerName,
                    numberSold,
                    companyName,
                    errMsg ));
            return false;
        }

        // All seems OK, now do the selling.
        StockSpace sellPrice;
        int price;

     // Get the sell price (does not change within a turn)
        if (sellPrices.containsKey(company)
                && GameOption.getAsBoolean(this, "SeparateSalesAtSamePrice")) {
            price = (sellPrices.get(company).getPrice());
        } else {
            sellPrice = company.getCurrentSpace();
            price = sellPrice.getPrice();
            sellPrices.put(company, sellPrice);
        }
        int cashAmount = ((numberSold * price * shareUnits)-(numberSold * 5)); //Deduct 5 Yuan per Sharecertificate Sold...

        // FIXME: changeStack.linkToPreviousMoveSet();
        String cashText = Currency.fromBank(cashAmount, currentPlayer);
        ReportBuffer.add(this, LocalText.getText("SELL_SHARES_LOG",
                playerName,
                numberSold,
                company.getShareUnit(),
                numberSold * company.getShareUnit(),
                companyName,
                cashText ));

        boolean soldBefore = sellPrices.containsKey(company);

        adjustSharePrice (company, numberSold, soldBefore);

        if (!company.isClosed()) {

            executeShareTransfer (company, certsToSell,
                    dumpedPlayer, presSharesToSell);
        }

        cashToRaise.add(-numberSold * price);
        
        if (cashToRaise.value() <= 0) {
            gameManager.finishShareSellingRound();
        } else if (getSellableShares().isEmpty()) {
            gameManager.finishShareSellingRound();
        }

        return true;        
    }
    
    // In 1880 all share transfers via ipo
    @Override
    protected void executeShareTransfer( PublicCompany company,
            List<PublicCertificate> certsToSell, 
            Player dumpedPlayer, int presSharesToSell) {
        
        executeShareTransferTo(company, certsToSell, dumpedPlayer, presSharesToSell, (BankPortfolio)ipo.getParent() );
    }

    
}
