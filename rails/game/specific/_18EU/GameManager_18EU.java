package rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.common.LocalText;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.Round;
import rails.game.ShareSellingRound;
import rails.game.model.MoneyModel;
import rails.game.model.PortfolioModel;
import rails.game.state.GenericState;
import rails.game.state.Item;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    protected final GenericState<Player> playerToStartFMERound =
       GenericState.create();

    @Override
    public void init(Item parent, String id){
        super.init(parent, id);
        playerToStartFMERound.init(this, "playerToStartFMERound");
    }
    
    @Override
    public void nextRound(Round round) {
        if (round instanceof OperatingRound_18EU) {
            if (playerToStartFMERound.get() != null
                    && relativeORNumber.value() == numOfORs.value()) {
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
        MoneyModel.cashMove(bankrupter, bank, bankrupter.getCashValue());
        PortfolioModel bpf = bankrupter.getPortfolioModel();
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
                ReportBuffer.add(LocalText.getText("CompanyCloses", company.getId()));
            }
        }
        
        // Dump all shares
        bankrupter.getPortfolioModel().getCertificatesModel().moveAll(bank.getPool().getCertificatesModel());
        bankrupter.setBankrupt();

        // Finish the share selling round
        if (getCurrentRound() instanceof ShareSellingRound) {
            finishShareSellingRound();
        }
    }
}
