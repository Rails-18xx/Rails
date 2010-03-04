package rails.game.specific._1835;

import java.util.ArrayList;
import java.util.List;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.PossibleAction;
import rails.game.special.ExchangeForShare;
import rails.game.special.SpecialPropertyI;
import rails.util.LocalText;

public class PrussianFormationRound extends StockRound {

    private PublicCompanyI prussian;
    private PhaseI phase;

	private boolean startPr;
	private boolean forcedStart;
	private boolean mergePr;
	private boolean forcedMerge;

    private enum Step {
        START,
        MERGE
    };

    Step step;

	public static String PR_ID = "Pr";
    public static String M2_ID = "M2";

    public PrussianFormationRound (GameManagerI gameManager) {
        super (gameManager);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);

    }

 	@Override
	public void start() {

        prussian = companyManager.getCompanyByName(PR_ID);
        phase = getCurrentPhase();
		startPr = !prussian.hasStarted();
        forcedStart = phase.getName().equals("4+4");
 		mergePr = !prussianIsComplete(gameManager);
 		forcedMerge = phase.getName().equals("5");

        ReportBuffer.add(LocalText.getText("StartFormationRound", PR_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
        		+" mergePr="+mergePr+" forcedMerge="+forcedMerge);

        step = startPr ? Step.START : Step.MERGE;
        if (step == Step.MERGE) {
        	startingPlayer
        		= ((GameManager_1835)gameManager).getPrussianFormationStartingPlayer();
        	setCurrentPlayer(startingPlayer);
        }
    }

    @Override
	public boolean setPossibleActions() {

        if (step == Step.START) {
            PublicCompanyI m2 = companyManager.getCompanyByName(M2_ID);
            Player m2Owner = m2.getPresident();
            startingPlayer = m2Owner;
            setCurrentPlayer(m2Owner);
            ReportBuffer.add(LocalText.getText("StartingPlayer",
                    getCurrentPlayer().getName()));

            possibleActions.add(new FoldIntoPrussian(m2));

        } else if (step == Step.MERGE) {

        	while (true) {
        		List<CompanyI> foldables = new ArrayList<CompanyI> ();
	            SpecialPropertyI sp;
	            for (PrivateCompanyI company : currentPlayer.getPortfolio().getPrivateCompanies()) {
	                sp = company.getSpecialProperties().get(0);
	                if (sp instanceof ExchangeForShare) {
	                    foldables.add(company);
	                }
	            }
	            PublicCompanyI company;
	            List<SpecialPropertyI> sps;
	            for (PublicCertificateI cert : currentPlayer.getPortfolio().getCertificates()) {
	                if (!cert.isPresidentShare()) continue;
	                company = cert.getCompany();
	                sps = company.getSpecialProperties();
	                if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
	                	foldables.add(company);
	                }
	            }
	            if (foldables.isEmpty()) {
	            	// No merge options for the current player, try the next one
	            	setNextPlayer();
	            	if (getCurrentPlayer() == startingPlayer) {
	            		finishRound();
	            		break;
	            	} else {
	            		continue;
	            	}
	            } else {
	                possibleActions.add(new FoldIntoPrussian(foldables));
	                break;
	            }
	        }
        }
        return true;

    }

    @Override
	protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof FoldIntoPrussian) {

            FoldIntoPrussian a = (FoldIntoPrussian) action;
            List<CompanyI> folded = a.getFoldedCompanies();

            if (step == Step.START) {
                if (folded.isEmpty() || !startPrussian(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                }
            } else if (step == Step.MERGE) {
                if (!folded.isEmpty()) mergeIntoPrussian (a);
            }

        	// No merge options for the current player, try the next one
        	setNextPlayer();
        	if (getCurrentPlayer() == startingPlayer) {
        		finishRound();
        	}

            return true;
        } else {
            return false;
        }
    }

    private boolean startPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        while (true) {
            if (!(M2_ID.equals(action.getFoldedCompanyNames()))) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        M2_ID);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false;
        }

        moveStack.start(false);

        // Execute
        prussian.start();
        prussian.setFloated();
        ((GameManager_1835)gameManager).setPrussianFormationStartingPlayer(currentPlayer);
        String message = LocalText.getText("START_MERGED_COMPANY",
                PR_ID,
                Bank.format(prussian.getIPOPrice()),
                prussian.getStartSpace());
        ReportBuffer.add(message);

        executeExchange (action.getFoldedCompanies(), true);

        return true;
    }

 private boolean mergeIntoPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        while (true) {
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false;
        }

        moveStack.start(false);

        // Execute
        executeExchange (action.getFoldedCompanies(), false);

        return true;
    }

 private void executeExchange (List<CompanyI> companies, boolean president) {

        ExchangeForShare efs;
        PublicCertificateI cert;
        for (CompanyI company : companies) {
            // Shortcut, sp should be checked
            efs = (ExchangeForShare) company.getSpecialProperties().get(0);
            cert = unavailable.findCertificate(prussian, efs.getShare()/prussian.getShareUnit(),
            		president);
            cert.moveTo(currentPlayer.getPortfolio());
            company.setClosed();
            ReportBuffer.add(LocalText.getText("MERGE_MINOR_LOG",
                    currentPlayer.getName(),
                    company.getName(),
                    PR_ID,
                    company instanceof PrivateCompanyI ? "no"
                            : Bank.format(((PublicCompanyI)company).getCash()),
                    company instanceof PrivateCompanyI ? "no"
                            : ((PublicCompanyI)company).getPortfolio().getTrainList().size()));
            ReportBuffer.add(LocalText.getText("GetShareForMinor",
                    currentPlayer.getName(),
                    cert.getShare(),
                    PR_ID,
                    ipo.getName(),
                    company.getName() ));
        }
    }

    public static boolean prussianIsComplete(GameManagerI gameManager) {
        List<PublicCertificateI> unissued
                = gameManager.getBank().getUnavailable().getCertificatesPerCompany(PR_ID);
        return unissued == null || unissued.isEmpty();
    }
    @Override
    public String toString() {
        return "1835 PrussianFormationRound";
    }

}
