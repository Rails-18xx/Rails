package rails.game.specific._1835;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.PossibleAction;
import rails.game.move.CashMove;
import rails.game.special.ExchangeForShare;
import rails.game.special.SpecialPropertyI;
import rails.util.LocalText;

public class PrussianFormationRound extends StockRound {

    private PublicCompanyI prussian;
    private PublicCompanyI m2;
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

	public static String PR_ID = StockRound_1835.PR_ID;
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
        forcedMerge = phase.getName().equals("5");
        forcedStart = phase.getName().equals("4+4") || forcedMerge;
 		mergePr = !prussianIsComplete(gameManager);

        ReportBuffer.add(LocalText.getText("StartFormationRound", PR_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
        		+" mergePr="+mergePr+" forcedMerge="+forcedMerge);

        step = startPr ? Step.START : Step.MERGE;
        
        if (step == Step.START) {
            m2 = companyManager.getCompanyByName(M2_ID);
            setCurrentPlayer(m2.getPresident());
            ((GameManager_1835)gameManager).setPrussianFormationStartingPlayer(currentPlayer);
            if (forcedStart) {
                executeStartPrussian(true);
                step = Step.MERGE;
            }
        }
        
        if (step == Step.MERGE) {
            startingPlayer
                    = ((GameManager_1835)gameManager).getPrussianFormationStartingPlayer();
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                List<SpecialPropertyI> sps;
                List<CompanyI> foldables = new ArrayList<CompanyI> ();
                for (PrivateCompanyI company : gameManager.getAllPrivateCompanies()) {
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
                    if (company.isClosed()) continue;
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                executeExchange (foldables, false, true);
                finishRound();
            }
        }
    }

    @Override
	public boolean setPossibleActions() {

        if (step == Step.START) {
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
 
            if (step == Step.START) {
                if (!startPrussian(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                }
            } else if (step == Step.MERGE) {
                
                mergeIntoPrussian (a);

            	// No merge options for the current player, try the next one
            	setNextPlayer();
            	if (getCurrentPlayer() == startingPlayer) {
            		finishRound();
            	}

            }
            return true;
        } else {
            return false;
        }
    }

    private boolean startPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        List<CompanyI> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
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

        if (folding) executeStartPrussian(false);

        return folding;
    }
    
    private void executeStartPrussian (boolean display) {
        
        prussian.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                PR_ID,
                Bank.format(prussian.getIPOPrice()),
                prussian.getStartSpace());
        ReportBuffer.add(message);
        if (display) DisplayBuffer.add(message);

        executeExchange (Arrays.asList(new CompanyI[]{m2}), true, false);
        prussian.setFloated();
    }

    private boolean mergeIntoPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        List<CompanyI> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();
        
        while (folding) {
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
        if (folding) executeExchange (action.getFoldedCompanies(), false, false);

        return folding;
    }

    private void executeExchange (List<CompanyI> companies, boolean president,
         boolean display) {

        ExchangeForShare efs;
        PublicCertificateI cert;
        Player player;
        for (CompanyI company : companies) {
            log.debug("Merging company "+company.getName());
            if (company instanceof PrivateCompanyI) {
                player = (Player)((PrivateCompanyI)company).getPortfolio().getOwner();
            } else {
                player = ((PublicCompanyI)company).getPresident();
            }
            // Shortcut, sp should be checked
            efs = (ExchangeForShare) company.getSpecialProperties().get(0);
            cert = unavailable.findCertificate(prussian, efs.getShare()/prussian.getShareUnit(),
            		president);
            cert.moveTo(player.getPortfolio());
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getName(),
                    company.getName(),
                    PR_ID,
                    company instanceof PrivateCompanyI ? "no"
                            : Bank.format(((PublicCompanyI)company).getCash()),
                    company instanceof PrivateCompanyI ? "no"
                            : ((PublicCompanyI)company).getPortfolio().getTrainList().size());
            ReportBuffer.add(message);
            if (display) DisplayBuffer.add (message);
            message = LocalText.getText("GetShareForMinor",
                    player.getName(),
                    cert.getShare(),
                    PR_ID,
                    ipo.getName(),
                    company.getName());
            ReportBuffer.add(message);
            if (display) DisplayBuffer.add (message);
            
            if (company instanceof PublicCompanyI) {

                PublicCompanyI minor = (PublicCompanyI) company;
        
                // Replace the home token
                BaseToken token = (BaseToken) minor.getTokens().get(0);
                City city = (City) token.getHolder();
                MapHex hex = city.getHolder();
                token.moveTo(minor);
                if (!hex.hasTokenOfCompany(prussian) && hex.layBaseToken(prussian, city.getNumber())) {
                    /* TODO: the false return value must be impossible. */
                    message =LocalText.getText("ExchangesBaseToken",
                            PR_ID, minor.getName(),
                            city.getName());
                            ReportBuffer.add(message);
                            if (display) DisplayBuffer.add (message);
                    
                    prussian.layBaseToken(hex, 0);
                }
        
                // Move any cash
                if (minor.getCash() > 0) {
                    new CashMove (minor, prussian, minor.getCash());
                }
        
                // Move any trains
                List<TrainI> trains = new ArrayList<TrainI> (minor.getPortfolio().getTrainList());
                for (TrainI train : trains) {
                    train.moveTo(prussian.getPortfolio());
                }
            }

            // Close the merged companies
            company.setClosed();
        }

        // Check the trains, autodiscard any excess non-permanent trains
        int trainLimit = prussian.getTrainLimit(gameManager.getCurrentPlayerIndex());
        List<TrainI> trains = prussian.getPortfolio().getTrainList();
        if (prussian.getNumberOfTrains() > trainLimit) {
            ReportBuffer.add("");
            int numberToDiscard = prussian.getNumberOfTrains() - trainLimit;
            List<TrainI> trainsToDiscard = new ArrayList<TrainI>(4);
            for (TrainI train : trains) {
                if (!train.getType().isPermanent()) {
                    trainsToDiscard.add(train);
                    if (--numberToDiscard == 0) break;
                }
            }
            for (TrainI train : trainsToDiscard) {
                train.moveTo(pool);
                ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                        PR_ID, train.getName()));
            }
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
