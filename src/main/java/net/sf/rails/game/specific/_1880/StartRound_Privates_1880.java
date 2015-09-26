package net.sf.rails.game.specific._1880;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import rails.game.action.PossibleAction;
import rails.game.specific._1880.SetupNewPublicDetails_1880;

public class StartRound_Privates_1880 extends StartRound_Sequential {

    private final GenericState<SetupNewPublicDetails_1880> pendingAction = 
            GenericState.create(this, "pendingAction");
    private final GenericState<Player> pendingPlayer = 
            GenericState.create(this, "pendingPlayer");
    private final GenericState<PublicCertificate> pendingCertificate = 
            GenericState.create(this, "pendingCertificate");

    public StartRound_Privates_1880(GameManager parent, String Id) {
        super(parent, Id);
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof SetupNewPublicDetails_1880) {

            SetupNewPublicDetails_1880 castAction =
                    (SetupNewPublicDetails_1880) action;
            Player player = playerManager.getCurrentPlayer();
            
            pendingCertificate.value().moveTo(player);
            ReportBuffer.add(this, LocalText.getText("ALSO_GETS", player.getId(),
                    pendingCertificate.value().toText()));
            
            PublicCompany_1880 company =
                    (PublicCompany_1880) castAction.getCompany();
            company.setBuildingRights(castAction.getBuildRightsString());
            
            ((GameManager_1880) gameManager).getParSlotManager().setCompanyAtIndex(
                    company, castAction.getParSlotIndex());
            ReportBuffer.add(this, LocalText.getText("BuildingRightsChosen",
                    player.getId(), castAction.getBuildRightsString(),
                    company.getId()));
            ReportBuffer.add(this, LocalText.getText("ParSlotChosen",
                    player.getId(), (castAction.getParSlotIndex() + 1),
                    company.getId()));
            company.start(castAction.getPrice());
            company.setFloated();
            Currency.wire(bank, 500, company);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",company.getId(), Bank.format(this, 500)));
            
            pendingAction.set(null);
            return true;
        }
        return super.process(action);
    }

    @Override
    protected void itemAssigned(Player player, StartItem item, int price) {
        if (item.hasSecondary()) {
            if (item.getSecondary() instanceof PublicCertificate) {
                PublicCertificate certificate =
                        (PublicCertificate) item.getSecondary();
                if (certificate.isPresidentShare() == true) {
                    playerManager.setCurrentPlayer(player);
                    pendingAction.set(
                            new SetupNewPublicDetails_1880(item,
                                    certificate.getCompany(), 100, 2)
                            );
                    pendingPlayer.set(player);
                    pendingCertificate.set(certificate);
                }
            }
        }
        item.setSold(player, price);
    }

    @Override
    public boolean setPossibleActions() {
        if (pendingAction.value() != null) {
            playerManager.setCurrentPlayer(pendingPlayer.value());
            possibleActions.add(pendingAction.value());
            return true;
        }
        return super.setPossibleActions();
    }

    @Override
    protected void finishRound() {
        Player firstPlayer = playerManager.reorderPlayersByCash(true);
        playerManager.setCurrentPlayer(firstPlayer);
        playerManager.setPriorityPlayer(firstPlayer);
   
        // report new player order
        List<String> playerNames = Lists.newArrayList();
        for (Player player:playerManager.getPlayers()) {
            playerNames.add(player.getId());
        }
        String players = Joiner.on(", ").join(playerNames);
        ReportBuffer.add(this, LocalText.getText("PlayersReordered", players));
        super.finishRound();
    }

}
