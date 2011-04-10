package rails.game.specific._1835;

import java.util.*;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.DiscardTrain;
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

    private List<CompanyI> foldablePrePrussians;

    private enum Step {
        START,
        MERGE,
        DISCARD_TRAINS
    };

    Step step;

	private static String PR_ID = GameManager_1835.PR_ID;
    private static String M2_ID = GameManager_1835.M2_ID;

    public PrussianFormationRound (GameManagerI gameManager) {
        super (gameManager);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);

    }

 	@Override
	public void start() {

        prussian = companyManager.getPublicCompany(PR_ID);
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
            m2 = companyManager.getPublicCompany(M2_ID);
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
            log.debug("Original Prussian starting player was "+startingPlayer.getName());
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                List<SpecialPropertyI> sps;
                setFoldablePrePrussians();
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
            } else {
                findNextMergingPlayer(false);
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

            possibleActions.add(new FoldIntoPrussian(foldablePrePrussians));

        } else if (step == Step.DISCARD_TRAINS) {

            if (prussian.getNumberOfTrains() > prussian.getTrainLimit(getCurrentPhase().getIndex())) {
                possibleActions.add(new DiscardTrain(prussian,
                        prussian.getPortfolio().getUniqueTrains(), true));
            }
        }
        return true;

    }

    private void setFoldablePrePrussians () {

        foldablePrePrussians = new ArrayList<CompanyI> ();
        SpecialPropertyI sp;
        for (PrivateCompanyI company : currentPlayer.getPortfolio().getPrivateCompanies()) {
            sp = company.getSpecialProperties().get(0);
            if (sp instanceof ExchangeForShare) {
                foldablePrePrussians.add(company);
            }
        }
        PublicCompanyI company;
        List<SpecialPropertyI> sps;
        for (PublicCertificateI cert : currentPlayer.getPortfolio().getCertificates()) {
            if (!cert.isPresidentShare()) continue;
            company = cert.getCompany();
            sps = company.getSpecialProperties();
            if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
                foldablePrePrussians.add(company);
            }
        }
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
                    findNextMergingPlayer(false);
                }

            } else if (step == Step.MERGE) {

                mergeIntoPrussian (a);

            }

            return true;

        } else if (action instanceof DiscardTrain) {

            discardTrain ((DiscardTrain) action);
            return true;

        } else {
            return false;
        }
    }

    protected boolean findNextMergingPlayer(boolean skipCurrentPlayer) {

        while (true) {

            if (skipCurrentPlayer) {
                setNextPlayer();
                if (getCurrentPlayer() == startingPlayer) {
                    if (prussian.getNumberOfTrains() > prussian.getTrainLimit(getCurrentPhase().getIndex())) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }

            setFoldablePrePrussians();
            if (!foldablePrePrussians.isEmpty()) return true;
            skipCurrentPlayer = true;
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
                        GameManager_1835.M2_ID);
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

        // all actions linked during formation round to avoid serious undo problems
        moveStack.start(false).linkToPreviousMoveSet();

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

        // add money from sold shares
        // Move cash and shares where required
        int capFactor = getSoldPercentage(prussian) / (prussian.getShareUnit() * prussian.getShareUnitsForSharePrice());
        int cash = capFactor * prussian.getIPOPrice();

        if (cash > 0) {
            new CashMove(bank, prussian, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                prussian.getName(),
                Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    prussian.getName()));
        }
        
        executeExchange (Arrays.asList(new CompanyI[]{m2}), true, false);
        prussian.setFloated();
    }

    private boolean mergeIntoPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        List<CompanyI> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            // TODO Some validation needed
            break;
        }

        // This is now dead code, but won't be when some sensible validations exist 
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false;
        }

        // all actions linked during formation round to avoid serious undo problems
        moveStack.start(false).linkToPreviousMoveSet();

        // Execute
        if (folding) executeExchange (folded, false, false);

        findNextMergingPlayer(true);

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
                    message = LocalText.getText("ExchangesBaseToken",
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

    }

    public boolean discardTrain(DiscardTrain action) {

        TrainI train = action.getDiscardedTrain();
        PublicCompanyI company = action.getCompany();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (company != prussian) {
                errMsg = LocalText.getText("WrongCompany", company.getName(), prussian.getName());
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?
            if (!company.getPortfolio().getTrainList().contains(train)) {
                errMsg =
                        LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getName(),
                                train.getName() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain",
                    company.getName(),
                    (train != null ?train.getName() : "?"),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);
        //
        if (action.isForced()) moveStack.linkToPreviousMoveSet();

        pool.buyTrain(train, 0);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                company.getName(),
                train.getName() ));

        // This always finished this type of round
        finishRound();

        return true;
    }

    @Override
    protected void finishRound() {
        RoundI interruptedRound = gameManager.getInterruptedRound();
        if (interruptedRound != null) {  
            ReportBuffer.add(LocalText.getText("EndOfFormationRound", PR_ID, 
                interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(LocalText.getText("EndOfFormationRoundNoInterrupt", PR_ID));
        }

        if (prussian.hasStarted()) prussian.checkPresidency();
        prussian.setOperated(); // To allow immediate share selling
        //        super.finishRound();
        // Inform GameManager
        gameManager.nextRound(this);
    }

    public static boolean prussianIsComplete(GameManagerI gameManager) {

        for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
            if (!company.getTypeName().equalsIgnoreCase("Minor")) continue;
            if (!company.isClosed()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "1835 PrussianFormationRound";
    }

}
