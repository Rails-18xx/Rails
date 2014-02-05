package rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.common.LocalText;
import rails.common.ReportBuffer;
import rails.game.Currency;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PlayerManager;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.RailsRoot;
import rails.game.Round;
import rails.game.ShareSellingRound;
import rails.game.model.PortfolioModel;
import rails.game.state.GenericState;
import rails.game.state.Portfolio;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    protected final GenericState<Player> playerToStartFMERound =
       GenericState.create(this, "playerToStartFMERound");
    
    public GameManager_18EU(RailsRoot parent, String id) {
        super(parent, id);
    }
    @Override
    public void nextRound(Round round) {
        if (round instanceof OperatingRound_18EU) {
            if (playerToStartFMERound.value() != null
                    && relativeORNumber.value() == numOfORs.value()) {
                // TODO: Fix the ID issue
                createRound (FinalMinorExchangeRound.class, "FinalMinorExchangeRound").start
                        ((Player)playerToStartFMERound.value());
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
        return (Player) playerToStartFMERound.value();
    }

    @Override
    protected void processBankruptcy () {
        Player player, newPresident;
        int numberOfPlayers = getRoot().getPlayerManager().getNumberOfPlayers();
        int maxShare;
        int share;

        // Assume default case as in 18EU: all assets to Bank/Pool
        Player bankrupter = getCurrentPlayer();
        Currency.toBankAll(bankrupter);
        PortfolioModel bpf = bankrupter.getPortfolioModel();
        List<PublicCompany> presidencies = new ArrayList<PublicCompany>();
        for (PublicCertificate cert : bpf.getCertificates()) {
            if (cert.isPresidentShare()) presidencies.add(cert.getCompany());
        }
        for (PublicCompany company : presidencies) {
            // Check if the presidency is dumped on someone
            newPresident = null;
            maxShare = 0;
            PlayerManager pm = getRoot().getPlayerManager();
            for (int index=pm.getCurrentPlayerIndex()+1;
            index<pm.getCurrentPlayerIndex()+numberOfPlayers; index++) {
                player = pm.getPlayerByIndex(index%numberOfPlayers);
                share = player.getPortfolioModel().getShare(company);
                if (share >= company.getPresidentsShare().getShare()
                        && (share > maxShare)) {
                    maxShare = share;
                    newPresident = player;
                }
            }
            if (newPresident != null) {
                bankrupter.getPortfolioModel().swapPresidentCertificate(company,
                        newPresident.getPortfolioModel());
            } else {
                company.setClosed();  // This also makes majors restartable
                ReportBuffer.add(this, LocalText.getText("CompanyCloses", company.getId()));
            }
        }
        
        // Dump all shares to pool
        Portfolio.moveAll(PublicCertificate.class, bankrupter, getRoot().getBank());
        bankrupter.setBankrupt();

        // Finish the share selling round
        if (getCurrentRound() instanceof ShareSellingRound) {
            finishShareSellingRound();
        }
    }
}
