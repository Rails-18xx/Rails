package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.Company;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Round;
import net.sf.rails.game.StockRound;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Train;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Currency;
import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import rails.game.specific._1837.FoldIntoKuK;

import com.google.common.collect.Iterables;

public class KuKFormationRound extends StockRound {

    private PublicCompany KuK;
    private PublicCompany h1;
    private Phase phase;

    private boolean startPr;
    private boolean forcedStart;
    private boolean mergePr;
    private boolean forcedMerge;

    private List<Company> foldablePreKuKs;

    private enum Step {
        START,
        MERGE,
        DISCARD_TRAINS
    };

    Step step;

    private static String KK_ID = GameManager_1837.KK_ID;
    private static String K1_ID = GameManager_1837.K1_ID;

    /**
     * Constructed via Configure
     */
    public KuKFormationRound (GameManager parent, String id) {
        super(parent, id);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
    }
    
    @Override
    public void start() {

        KuK = companyManager.getPublicCompany(KK_ID);
        phase = getCurrentPhase();
        startPr = !KuK.hasStarted();
        forcedMerge = phase.getId().equals("5");
        forcedStart = phase.getId().equals("4+4") || forcedMerge;
        mergePr = !KuKIsComplete(gameManager);

        ReportBuffer.add(this, LocalText.getText("StartFormationRound", KK_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
                +" mergePr="+mergePr+" forcedMerge="+forcedMerge);

        step = startPr ? Step.START : Step.MERGE;

        if (step == Step.START) {
            h1 = companyManager.getPublicCompany(K1_ID);
            setCurrentPlayer(h1.getPresident());
            ((GameManager_1837)gameManager).setKuKFormationStartingPlayer(currentPlayer);
            if (forcedStart) {
                executeStartKuK(true);
                step = Step.MERGE;
            }
        }

        if (step == Step.MERGE) {
            startingPlayer
            = ((GameManager_1837)gameManager).getKuKFormationStartingPlayer();
            log.debug("Original KuK starting player was "+startingPlayer.getId());
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                Set<SpecialProperty> sps;
                setFoldablePreKuKs();
                List<Company> foldables = new ArrayList<Company> ();
                for (PrivateCompany company : gameManager.getAllPrivateCompanies()) {
                    if (company.isClosed()) continue;
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                for (PublicCompany company : gameManager.getAllPublicCompanies()) {
                    if (company.isClosed()) continue;
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                executeExchange (foldables, false, true);

                // Check if the PR must discard any trains
                if (KuK.getNumberOfTrains() > KuK.getCurrentTrainLimit()) {
                    step = Step.DISCARD_TRAINS;
                } else {
                    finishRound();
                }
            } else {
                findNextMergingPlayer(false);
            }
        }
    }

    @Override
    public boolean setPossibleActions() {

        if (step == Step.START) {
            Player h1Owner = h1.getPresident();
            startingPlayer = h1Owner;
            setCurrentPlayer(h1Owner);
            ReportBuffer.add(this, LocalText.getText("StartingPlayer", 
                    playerManager.getCurrentPlayer().getId()
                    ));

            possibleActions.add(new FoldIntoKuK(h1));

        } else if (step == Step.MERGE) {

            possibleActions.add(new FoldIntoKuK(foldablePreKuKs));

        } else if (step == Step.DISCARD_TRAINS) {

            if (KuK.getNumberOfTrains() > KuK.getCurrentTrainLimit()) {
                log.debug("+++ PR has "+KuK.getNumberOfTrains()+", limit is "+KuK.getCurrentTrainLimit());
                possibleActions.add(new DiscardTrain(KuK,
                        KuK.getPortfolioModel().getUniqueTrains(), true));
            }
        }
        return true;

    }

    private void setFoldablePreKuKs () {

        foldablePreKuKs = new ArrayList<Company> ();
        SpecialProperty sp;
        for (PrivateCompany company : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
            sp = Iterables.get(company.getSpecialProperties(), 0);
            if (sp instanceof ExchangeForShare) {
                foldablePreKuKs.add(company);
            }
        }
        PublicCompany company;
        Set<SpecialProperty> sps;
        for (PublicCertificate cert : currentPlayer.getPortfolioModel().getCertificates()) {
            if (!cert.isPresidentShare()) continue;
            company = cert.getCompany();
            sps = company.getSpecialProperties();
            if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                foldablePreKuKs.add(company);
            }
        }
    }

    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof FoldIntoKuK) {

            FoldIntoKuK a = (FoldIntoKuK) action;

            if (step == Step.START) {
                if (!startKuK(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                    findNextMergingPlayer(false);
                }

            } else if (step == Step.MERGE) {

                mergeIntoKuK (a);

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
                if (playerManager.getCurrentPlayer() == startingPlayer) {
                    if (KuK.getNumberOfTrains() > KuK.getCurrentTrainLimit()) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }

            setFoldablePreKuKs();
            if (!foldablePreKuKs.isEmpty()) return true;
            skipCurrentPlayer = true;
        }
    }

    private boolean startKuK (FoldIntoKuK action) {

        // Validate
        String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            if (!(K1_ID.equals(action.getFoldedCompanyNames()))) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        GameManager_1837.K1_ID);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    KK_ID,
                    errMsg));
            return false;
        }

        // all actions linked during formation round to avoid serious undo problems
        
        // FIXME: changeStack.linkToPreviousMoveSet();

        if (folding) executeStartKuK(false);

        return folding;
    }

    private void executeStartKuK (boolean display) {

        KuK.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                KK_ID,
                Bank.format(this, KuK.getIPOPrice()),
                KuK.getStartSpace().toText());
        ReportBuffer.add(this, message);
        if (display) DisplayBuffer.add(this, message);

        // add money from sold shares
        // Move cash and shares where required
        int capFactor = KuK.getSoldPercentage() / (KuK.getShareUnit() * KuK.getShareUnitsForSharePrice());
        int cash = capFactor * KuK.getIPOPrice();

        if (cash > 0) {
            String cashText = Currency.fromBank(cash, KuK);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",
                KuK.getId(),
                cashText ));
        } else {
            ReportBuffer.add(this, LocalText.getText("Floats",
                    KuK.getId()));
        }

        executeExchange (Arrays.asList(new Company[]{h1}), true, false);
        KuK.setFloated();
    }

    private boolean mergeIntoKuK (FoldIntoKuK action) {

        // Validate
        // String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            // TODO Some validation needed
            break;
        }

        // TODO: This is now dead code, but won't be when some sensible validations exist 
        /*
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false; 
        }
        */

        // all actions linked during formation round to avoid serious undo problems
        
        // FIMXE: changeStack.linkToPreviousMoveSet();

        // Execute
        if (folding) executeExchange (folded, false, false);

        findNextMergingPlayer(true);

        return folding;
    }

    private void executeExchange (List<Company> companies, boolean president,
            boolean display) {

        ExchangeForShare efs;
        PublicCertificate cert;
        Player player;
        for (Company company : companies) {
            log.debug("Merging company "+company.getId());
            if (company instanceof PrivateCompany) {
                player = (Player)((PrivateCompany)company).getOwner();
            } else {
                player = ((PublicCompany)company).getPresident();
            }
            // Shortcut, sp should be checked
            efs = (ExchangeForShare) Iterables.get(company.getSpecialProperties(), 0);
            cert = unavailable.findCertificate(KuK, efs.getShare()/KuK.getShareUnit(),
                    president);
            cert.moveTo(player);
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getId(),
                    company.getId(),
                    KK_ID,
                    company instanceof PrivateCompany ? "no"
                            : Bank.format(this, ((PublicCompany)company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany)company).getPortfolioModel().getTrainList().size());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);
            message = LocalText.getText("GetShareForMinor",
                    player.getId(),
                    cert.getShare(),
                    KK_ID,
                    ipo.getParent().getId(),
                    company.getId());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);

            if (company instanceof PublicCompany) {

                PublicCompany minor = (PublicCompany) company;

                // Replace the home token
                BaseToken token = Iterables.get(minor.getAllBaseTokens(),0);
                Stop city = (Stop) token.getOwner();
                MapHex hex = city.getParent();
                token.moveTo(minor);
                if (!hex.hasTokenOfCompany(KuK) && hex.layBaseToken(KuK, city)) {
                    /* TODO: the false return value must be impossible. */
                    message = LocalText.getText("ExchangesBaseToken",
                            KK_ID, minor.getId(),
                            city.getSpecificId());
                    ReportBuffer.add(this, message);
                    if (display) DisplayBuffer.add(this, message);

                    KuK.layBaseToken(hex, 0);
                }

                // Move any cash
                if (minor.getCash() > 0) {
                    Currency.wireAll(minor, KuK);
                }

                // Move any trains
                // TODO: Simplify code due to trainlist being immutable anyway
                List<Train> trains = new ArrayList<Train> (minor.getPortfolioModel().getTrainList());
                for (Train train : trains) {
                    KuK.getPortfolioModel().addTrain(train);
                }
            }

            // Close the merged companies
            company.setClosed();
        }

    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (company != KuK) {
                errMsg = LocalText.getText("WrongCompany", company.getId(), KuK.getId());
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?
            if (!company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                    LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(),
                                train.toText() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    company.getId(),
                    (train != null ?train.toText() : "?"),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        
        // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();

        pool.addTrain(train);
        ReportBuffer.add(this, LocalText.getText("CompanyDiscardsTrain",
                company.getId(),
                train.toText() ));

        // We still might have another excess train
        // TODO: would be better to have DiscardTrain discard multiple trains
        if (KuK.getNumberOfTrains() > KuK.getCurrentTrainLimit()) {
            step = Step.DISCARD_TRAINS;
        } else {
            finishRound();
        }

        return true;
    }

    @Override
    protected void finishRound() {
        Round interruptedRound = gameManager.getInterruptedRound();
        ReportBuffer.add(this, " ");
        if (interruptedRound != null) {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRound", KK_ID,
                    interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRoundNoInterrupt", KK_ID));
        }

        if (KuK.hasStarted()) KuK.checkPresidency();
        KuK.setOperated(); // To allow immediate share selling
        //        super.finishRound();
        // Inform GameManager
        gameManager.nextRound(this);
    }

    public static boolean KuKIsComplete(GameManager gameManager) {

        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (!company.getType().getId().equalsIgnoreCase("Minor")) continue;
            if (!company.isClosed()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "1837 KuKFormationRound";
    }
}