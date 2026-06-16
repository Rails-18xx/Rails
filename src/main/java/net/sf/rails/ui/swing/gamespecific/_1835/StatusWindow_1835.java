package net.sf.rails.ui.swing.gamespecific._1835;

import java.util.List;

import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import rails.game.specific._1835.FoldIntoPrussian;
import rails.game.specific._1835.StartPrussian;
import rails.game.specific._1835.ExchangeForPrussianShare;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.specific._1835.*;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.StatusWindow;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.financial.PublicCertificate;

import com.google.common.collect.Iterables;

public class StatusWindow_1835 extends StatusWindow {

    private PublicCompany prussian;

    private static final long serialVersionUID = 1L;

    public StatusWindow_1835() {
        super();
    }

    @Override
    public void init (GameUIManager gameUIManager) {
        super.init(gameUIManager);
        prussian = gameUIManager.getRoot().getCompanyManager().getPublicCompany(GameDef_1835.PR_ID);
    }

    @Override
    public void updateStatus(boolean myTurn) {
        RoundFacade currentRound = gameUIManager.getCurrentRound();
        
        // 1. Check for Immediate Actions specific to PFR
        if (currentRound instanceof PrussianFormationRound) {
            if (possibleActions.contains(FoldIntoPrussian.class)) {
                immediateAction = possibleActions.getType(FoldIntoPrussian.class).get(0);
            } else if (possibleActions.contains(DiscardTrain.class)) {
                immediateAction = possibleActions.getType(DiscardTrain.class).get(0);
            }
        }

        // 2. Run the standard update (which will call our updateGameSpecificHighlights hook)
        super.updateStatus(myTurn);
    }

    @Override
    protected void updateGameSpecificHighlights() {
        if (possibleActions == null) return;
        
        RoundFacade currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof PrussianFormationRound)) return;

        for (PossibleAction pa : possibleActions.getList()) {
            Certificate targetCert = null;
            String tip = "Select";

            // Case 1: Start Prussian (M2 Director)
            if (pa instanceof StartPrussian) {
                Company m2 = gameUIManager.getRoot().getCompanyManager().getPublicCompany(GameDef_1835.M2_ID);
                if (m2 instanceof PublicCompany) {
                    targetCert = ((PublicCompany) m2).getPresidentsShare();
                }
                tip = "Fold M2 into Prussian";
            }
            
            // Case 2: Exchange Share (Generic)
            else if (pa instanceof ExchangeForPrussianShare) {
                ExchangeForPrussianShare eps = (ExchangeForPrussianShare) pa;
                Company targetCo = eps.getCompanyToExchange();
                
                if (targetCo != null) {
                    Player p = getCurrentPlayer();
                    if (p != null) {
                        for (Certificate c : p.getPortfolioModel().getCertificates()) {
                            boolean match = false;
                            
                            if (c instanceof PublicCertificate) {
                                // Public/Minor: Compare the certificate's company to the target
                                match = ((PublicCertificate) c).getCompany() == targetCo;
                            } else if (c instanceof PrivateCompany) {
                                // Private: The certificate IS the company. Compare directly.
                                match = c == targetCo;
                            }

                            if (match) {
                                targetCert = c; 
                                break; 
                            }
                        }
                    }
                }
                tip = "Exchange for Prussian";
            }

            // EXECUTE THE HIGHLIGHT
            if (targetCert != null) {
                // This method is inherited from StatusWindow
                attachActionToCertificate(targetCert, pa, tip);
            }
        }
    }

    @Override
    public boolean processImmediateAction() {
        if (immediateAction == null) {
            return false;
        } else if (immediateAction instanceof FoldIntoPrussian) {
            FoldIntoPrussian nextAction = (FoldIntoPrussian) immediateAction;
            immediateAction = null;
            fold (nextAction);
            return true;
        } else {
            return super.processImmediateAction();
        }
    }

    protected void fold (FoldIntoPrussian action) {
        List<Company> foldables = action.getFoldableCompanies();
        NonModalDialog currentDialog;

        if (foldables.get(0).getId().equals(GameDef_1835.M2_ID)) {
            // Ask if the Prussian should be started
            currentDialog = new ConfirmationDialog (GameUIManager_1835.START_PRUSSIAN_DIALOG,
                    gameUIManager, this,
                    LocalText.getText("Select"),
                    LocalText.getText("MergeMinorConfirm",
                            getCurrentPlayer().getId(),
                            GameDef_1835.M2_ID, GameDef_1835.PR_ID),
                            "Yes",
                            "No"
            );
        } else {
            // Ask if any other prePrussians should be folded
            String[] options = new String[foldables.size()];
            Company company;
            for (int i=0; i<options.length; i++) {
                company = foldables.get(i);
                options[i] = LocalText.getText("MergeOption",
            			company.getId(),
                        company.getLongName(),
            			prussian.getId(),
            			((ExchangeForShare)(Iterables.get(company.getSpecialProperties(),0))).getShare()
                );
            }
            currentDialog = new CheckBoxDialog (GameUIManager_1835.MERGE_INTO_PRUSSIAN_DIALOG,
                    gameUIManager,
                    this,
                    LocalText.getText("Select"),
                    LocalText.getText("SelectCompaniesToFold",
                            getCurrentPlayer().getId(),
                            prussian.getLongName()),
                            options);
        }
        gameUIManager.setCurrentDialog (currentDialog, action);
        disableButtons();
    }
}