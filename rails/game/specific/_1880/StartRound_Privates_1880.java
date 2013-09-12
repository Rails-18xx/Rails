package rails.game.specific._1880;

import rails.common.LocalText;
import rails.game.GameManagerI;
import rails.game.Player;
import rails.game.PublicCertificateI;
import rails.game.ReportBuffer;
import rails.game.StartItem;
import rails.game.action.PossibleAction;

public class StartRound_Privates_1880 extends StartRound_Sequential {

    private SetupNewPublicDetails_1880 pendingAction = null;
    private int pendingPlayerIndex = 0;
    private PublicCertificateI pendingCertificate = null;

    public StartRound_Privates_1880(GameManagerI gameManager) {
        super(gameManager);
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof SetupNewPublicDetails_1880) {
            SetupNewPublicDetails_1880 castAction =
                    (SetupNewPublicDetails_1880) action;
            Player player = getCurrentPlayer();
            transferCertificate(pendingCertificate, player.getPortfolio());
            ReportBuffer.add(LocalText.getText("ALSO_GETS", player.getName(),
                    pendingCertificate.getName()));
            PublicCompany_1880 company =
                    (PublicCompany_1880) castAction.getCompany();
            company.setBuildingRights(castAction.getBuildRightsString());
            company.start(castAction.getPrice());
            ((GameManager_1880) gameManager).getParSlots().setCompanyAtSlot(
                    company, castAction.getParSlotIndex());
            ReportBuffer.add(LocalText.getText("BuildingRightsChosen", player.getName(), 
                    castAction.getBuildRightsString(), company.getName()));
            ReportBuffer.add(LocalText.getText("ParSlotChosen", player.getName(), 
                    (castAction.getParSlotIndex()+1), company.getName()));

            pendingAction = null;
            return true;
        }
        return super.process(action);
    }

    @Override
    protected void itemAssigned(Player player, StartItem item, int price) {
        if (item.hasSecondary()) {
            if (item.getSecondary() instanceof PublicCertificateI) {
                PublicCertificateI certificate =
                        (PublicCertificateI) item.getSecondary();
                if (certificate.isPresidentShare() == true) {
                    gameManager.setCurrentPlayer(player);
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
        gameManager.setCurrentPlayer(gameManager.reorderPlayersByCash(true));
        currentPlayer=getCurrentPlayer();
        gameManager.setPriorityPlayer((Player) currentPlayer); // Method doesn't exist in Startround ???
        ReportBuffer.add(LocalText.getText("PlayersReordered"));
        super.finishRound();
    }
    
}
