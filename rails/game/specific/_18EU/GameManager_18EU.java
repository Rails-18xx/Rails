/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/GameManager_18EU.java,v 1.5 2010/01/18 22:51:47 evos Exp $ */
package rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.common.LocalText;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.RoundI;
import rails.game.ShareSellingRound;
import rails.game.model.Owners;
import rails.game.model.PortfolioModel;
import rails.game.state.Context;
import rails.game.state.GenericState;
import rails.util.Util;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    protected GenericState<Player> playerToStartFMERound =
       GenericState.create(this, "playerToStartFMERound");

    public GameManager_18EU(Context context) {
        super(context);
    }
    
    @Override
    public void nextRound(RoundI round) {
        if (round instanceof OperatingRound_18EU) {
            if (playerToStartFMERound.get() != null
                    && relativeORNumber.intValue() == numOfORs.intValue()) {
                createRound (FinalMinorExchangeRound.class).start
                        ((Player)playerToStartFMERound.get());
                playerToStartFMERound.set(null);
            } else {
                super.nextRound(round);
            }
        } else if (round instanceof FinalMinorExchangeRound) {
            startStockRound();
        } else {
            super.nextRound(round);
        }

    }

    public void setPlayerToStartFMERound(Player playerToStartFMERound) {
        this.playerToStartFMERound.set(playerToStartFMERound);
    }

    public Player getPlayerToStartFMERound() {
        return (Player) playerToStartFMERound.get();
    }

    @Override
    protected void processBankruptcy () {
        Player player, newPresident;
        int numberOfPlayers = getNumberOfPlayers();
        int maxShare;
        int share;

        // Assume default case as in 18EU: all assets to Bank/Pool
        Player bankrupter = getCurrentPlayer();
        Owners.cashMove(bankrupter, bank, bankrupter.getCashValue());
        PortfolioModel bpf = bankrupter.getPortfolio();
        List<PublicCompany> presidencies = new ArrayList<PublicCompany>();
        for (PublicCertificate cert : bpf.getCertificates()) {
            if (cert.isPresidentShare()) presidencies.add(cert.getCompany());
        }
        for (PublicCompany company : presidencies) {
            // Check if the presidency is dumped on someone
            newPresident = null;
            maxShare = 0;
            for (int index=getCurrentPlayerIndex()+1;
            index<getCurrentPlayerIndex()+numberOfPlayers; index++) {
                player = getPlayerByIndex(index%numberOfPlayers);
                share = player.getPortfolio().getShare(company);
                if (share >= company.getPresidentsShare().getShare()
                        && (share > maxShare)) {
                    maxShare = share;
                    newPresident = player;
                }
            }
            if (newPresident != null) {
                bankrupter.getPortfolio().swapPresidentCertificate(company,
                        newPresident.getPortfolio());
            } else {
                company.setClosed();  // This also makes majors restartable
                ReportBuffer.add(LocalText.getText("CompanyCloses", company.getId()));
            }
        }
        
        // Dump all shares
        Owners.moveAll(bankrupter, bank.getPool(), PublicCertificate.class);

        bankrupter.setBankrupt();

        // Finish the share selling round
        if (getCurrentRound() instanceof ShareSellingRound) {
            finishShareSellingRound();
        }
    }
}
