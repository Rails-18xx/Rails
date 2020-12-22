package net.sf.rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Portfolio;


/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager_18EU extends GameManager {

    protected final GenericState<Player> playerToStartFMERound = new GenericState<>(this, "playerToStartFMERound");

    public GameManager_18EU(RailsRoot parent, String id) {
        super(parent, id);
    }

    @Override
    public void nextRound(Round round) {
        if (round instanceof OperatingRound_18EU) {
            if (playerToStartFMERound.value() != null
                    && relativeORNumber.value() == numOfORs.value()) {
                // TODO: Fix the ID issue
                createRound(FinalMinorExchangeRound.class, "FinalMinorExchangeRound")
                        .start (playerToStartFMERound.value());
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
        return playerToStartFMERound.value();
    }

    @Override
    protected void processCompanyAfterPlayerBankruptcy(Player player, PublicCompany company) {
        company.setClosed();  // This also makes majors restartable
        ReportBuffer.add(this, LocalText.getText("CompanyCloses", company.getId()));
    }

}
