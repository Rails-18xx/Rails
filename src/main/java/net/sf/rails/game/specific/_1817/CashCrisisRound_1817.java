package net.sf.rails.game.specific._1817;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.Round;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the 1817 Rule 7.4 Cash Crisis.
 * Forces a player to sell non-president shares until their cash is >= 0.
 * Triggers bankruptcy if insufficient funds can be raised.
 */
public class CashCrisisRound_1817 extends Round {
    private static final Logger log = LoggerFactory.getLogger(CashCrisisRound_1817.class);
    private final net.sf.rails.game.state.GenericState<Player> crisisPlayer;

    public CashCrisisRound_1817(GameManager parent, String id) {
        super(parent, id);
        crisisPlayer = new net.sf.rails.game.state.GenericState<>(this, "crisisPlayer", null);
    }

    public void start(Player p) {
        crisisPlayer.set(p);
        log.warn("CASH CRISIS: " + p.getName() + " entered Cash Crisis with balance $" + p.getCash());
        gameManager.getRoot().getPlayerManager().setCurrentPlayer(p);
    }

    @Override
    public boolean setPossibleActions() {
        possibleActions.clear();
        Player p = crisisPlayer.value();

        if (p == null) {
            gameManager.nextRound(this);
            return true;
        }

        // Crisis is resolved once the player climbs out of negative cash
        if (p.getCash() >= 0) {
            log.info("CASH CRISIS: " + p.getName() + " resolved the crisis.");
            net.sf.rails.common.ReportBuffer.add(this, p.getName() + " resolves the Cash Crisis.");
            gameManager.nextRound(this);
            return true;
        }

        boolean canSell = false;
        for (PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
            PublicCompany comp = cert.getCompany();
            
            // Rule 7.4 Constraints:
            // 1. Subject to 5.2 limitations: Cannot sell 2-share companies.
            boolean isTwoShare = (comp instanceof PublicCompany_1817 && ((PublicCompany_1817)comp).getShareCount() == 2);
            // 2. Shares may not be sold that cause a president's certificate to change hands.
            boolean isPres = cert.isPresidentShare();
            // 3. Cannot sell shorts (they are liabilities).
            boolean isShort = cert instanceof net.sf.rails.game.specific._1817.ShortCertificate;
            // 4. Cannot sell companies in acquisition/liquidation
            boolean isBuyable = comp.isBuyable(); 

            if (!isTwoShare && !isPres && !isShort && isBuyable) {
                possibleActions.add(new SellShares(comp, comp.getShareUnit(), 1, comp.getMarketPrice()));
                canSell = true;
            }
        }

        // If no legal sales remain and cash is still negative, the player is bankrupt.
        if (!canSell) {
            log.warn("CASH CRISIS: " + p.getName() + " cannot raise enough funds. BANKRUPTCY triggered.");
            net.sf.rails.common.ReportBuffer.add(this, p.getName() + " cannot raise enough funds and declares BANKRUPTCY!");
            executeBankruptcy(p);
            gameManager.nextRound(this);
        }

        return true;
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof SellShares) {
            SellShares sellAction = (SellShares) action;
            PublicCompany comp = sellAction.getCompany();
            Player p = crisisPlayer.value();

            int price = comp.getMarketPrice();
            
            // Find a single legal non-president share to move
            PublicCertificate certToSell = null;
            for (PublicCertificate cert : p.getPortfolioModel().getCertificates(comp)) {
                if (!cert.isPresidentShare() && !(cert instanceof net.sf.rails.game.specific._1817.ShortCertificate)) {
                    certToSell = cert;
                    break;
                }
            }

            if (certToSell != null) {
                certToSell.moveTo(gameManager.getRoot().getBank().getPool());
                net.sf.rails.game.state.Currency.fromBank(price, p);
                
                net.sf.rails.common.ReportBuffer.add(this, p.getName() + " sells 1 share of " + comp.getId() + " for $" + price + " to raise funds.");
                
                setPossibleActions();
                return true;
            }
        }
        return super.process(action);
    }

    private void executeBankruptcy(Player p) {
        net.sf.rails.game.state.Owner poolOwner = gameManager.getRoot().getBank().getPool();
        
        // Dump all holdings to the Open Market
        for (PublicCertificate cert : new java.util.ArrayList<>(p.getPortfolioModel().getCertificates())) {
            cert.moveTo(poolOwner);
            
            // If a president's certificate hits the open market, liquidate the company
            if (cert.isPresidentShare()) {
                PublicCompany comp = cert.getCompany();
                net.sf.rails.game.financial.StockMarket market = gameManager.getRoot().getStockMarket();
                net.sf.rails.game.financial.StockSpace liqSpace = market.getStartSpace(0);
                if (liqSpace != null && comp.getCurrentSpace() != liqSpace) {
                    market.correctStockPrice(comp, liqSpace);
                    net.sf.rails.common.ReportBuffer.add(this, comp.getId() + " is placed in liquidation due to president's bankruptcy.");
                }
            }
        }
        
        // Dump Private Companies to the Open Market
        for (net.sf.rails.game.PrivateCompany priv : new java.util.ArrayList<>(p.getPortfolioModel().getPrivateCompanies())) {
            priv.moveTo(poolOwner);
        }
        
        if (p.getCash() > 0) {
            net.sf.rails.game.state.Currency.toBank(p, p.getCash());
        }

        // Standard Rails method to remove player from active order
        p.setBankrupt();
    }
}