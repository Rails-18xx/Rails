package net.sf.rails.game.specific._1880;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Currency;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.state.ChangeStack;
import rails.game.action.PossibleAction;

public class StartRound_Privates_1880 extends StartRound_Sequential {

    private SetupNewPublicDetails_1880 pendingAction = null;
    private int pendingPlayerIndex = 0;
    private PublicCertificate pendingCertificate = null;

    public StartRound_Privates_1880(GameManager parent, String Id) {
        super(parent, Id);
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof SetupNewPublicDetails_1880) {
            SetupNewPublicDetails_1880 castAction =
                    (SetupNewPublicDetails_1880) action;
            Player player = getCurrentPlayer();
            
            transferCertificate(pendingCertificate, player.getPortfolioModel());
            ReportBuffer.add(this, LocalText.getText("ALSO_GETS", player.getId(),
                    pendingCertificate.getName()));
            PublicCompany_1880 company =
                    (PublicCompany_1880) castAction.getCompany();
            company.setBuildingRights(castAction.getBuildRightsString());
            
            ((GameManager_1880) gameManager).getParSlotManager().setCompanyAtSlot(
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
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",company.getId(), Currency.format(this, 500)));
            
            
            pendingAction = null;
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
                    getRoot().getPlayerManager().setCurrentPlayer(player);
                    pendingAction =
                            new SetupNewPublicDetails_1880(item,
                                    certificate.getCompany(), 100, 2);
                    pendingPlayerIndex = player.getIndex();
                    pendingCertificate = certificate;
                }
            }
        }
        item.setSold(player, price);
    }

    @Override
    public boolean setPossibleActions() {
        if (pendingAction != null) {
            setCurrentPlayerIndex(pendingPlayerIndex);
            possibleActions.add(pendingAction);
            return true;
        }
        return super.setPossibleActions();
    }

    @Override
    protected void finishRound() {
        getRoot().getPlayerManager().setCurrentPlayer(getRoot().getPlayerManager().reorderPlayersByCash(true));
        currentPlayer = getCurrentPlayer();
        getRoot().getPlayerManager().setPriorityPlayer((Player) currentPlayer); // Method doesn't
                                                               // exist in
                                                               // Startround ???
        ReportBuffer.add(this, LocalText.getText("PlayersReordered"));
        super.finishRound();
    }

}
