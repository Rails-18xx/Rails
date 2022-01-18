package net.sf.rails.game.specific._1837;

import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * @author martin, erik
 *
 */
public class GameManager_1837 extends GameManager {

    private static final Logger log = LoggerFactory.getLogger(GameManager_1837.class);

    private StringState newPhaseId = StringState.create(this, "newPhaseId", null);

    protected final GenericState<Round> previousSRorOR =
            new GenericState<> (this, "previousSRorOR");

    private SetState<String> doneThisRound = HashSetState.create (this, "doneThisRound");

    protected final BooleanState buyOnly =
            new BooleanState(this, "buyOnly", false);

    protected CompanyManager companyManager;
    protected PhaseManager phaseManager;

    public GameManager_1837(RailsRoot parent, String id) {

        super(parent, id);
    }

    public void init() {
        super.init();
        companyManager = getRoot().getCompanyManager();
        phaseManager = getRoot().getPhaseManager();
    }

    @Override
    public void newPhaseChecks(RoundFacade round) {
        newPhaseId.set(round.getId());
    }

    public void nextRound(Round prevRound) {

        if (prevRound instanceof StartRound) {
            // In version 2, any subsequent start round will be buy-only (no bidding).
            buyOnly.set(true);
            if (((StartRound) prevRound).getStartPacket().areAllSold()) { // This start round was "completed"
                // check if there are other StartPackets, otherwise stockRounds start
                beginStartRound();
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
        } else if (prevRound instanceof CoalExchangeRound) {
            //Since the CoalExchangeRound can happen after both types of rounds we need to move the
            //round decision down to this class and cant call the superclass :(

            doneThisRound.add("CER");
            if (checkAndRunNFR(newPhaseId.value(), previousSRorOR.value(), (Round)getInterruptedRound())) {
                return;
            } else if (previousSRorOR.value() instanceof StockRound) {
                // Start the first OR after an SR.
                Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
                if (currentPhase == null) log.error("Current Phase is null??", new Exception(""));
                numOfORs.set(currentPhase.getNumberOfOperatingRounds());
                log.debug("Phase={} ORs={}", currentPhase.toText(), numOfORs.value());

                // Create a new OperatingRound (never more than one Stock Round)
                relativeORNumber.set(0);
                startOperatingRound(true);
            } else if (relativeORNumber.value() < numOfORs.value()) {
                // There will be another OR
                startOperatingRound(true);
            } else {
                startStockRound();
            }

            getCurrentRound().setPossibleActions();

        } else if (prevRound instanceof NationalFormationRound) {
            doneThisRound.add(((NationalFormationRound) prevRound).getNational().getId());
            OperatingRound_1837 interruptedRound = (OperatingRound_1837) getInterruptedRound();
            if (checkAndRunNFR(newPhaseId.value(), previousSRorOR.value(), interruptedRound)) {
                return;
            }

            if (interruptedRound != null) {
                setRound(interruptedRound);
                interruptedRound.resume();
            } else {
                super.nextRound(previousSRorOR.value());
            }
        } else if (prevRound instanceof StockRound_1837 || prevRound instanceof OperatingRound_1837) {
            previousSRorOR.set (prevRound); // Remember where we came from!
            doneThisRound.clear();
            setInterruptedRound(prevRound);
            if (!checkAndRunCER(null, prevRound, null)
                    && !checkAndRunNFR(null, prevRound, null)) {
                super.nextRound(prevRound);
            }
        } else {
            setInterruptedRound(null);
            super.nextRound(prevRound);
        }
    }

    public boolean checkAndRunCER(String newPhaseId, Round namingRound, Round interruptedRound) {
        if (doneThisRound.contains("CER")) return false;
        List<PublicCompany> coalCompanies =
                getRoot().getCompanyManager().getPublicCompaniesByType("Coal");
        boolean runCER = false;
        for (PublicCompany coalComp : coalCompanies) {
            if (!coalComp.isClosed()
                    && coalComp.getRelatedPublicCompany().hasFloated()) {
                runCER = true;
                setInterruptedRound(interruptedRound);
                setNewPhaseId(newPhaseId);
                break;
            }
        }
        if (runCER) {
            //CoalRoundFollowedByOR.set(prevRound instanceof StockRound_1837);
            // Number the CER with the numeric part of the previous round.
            // After SR_n: CER_n.0
            // After OR_n.m: CER_n.m; if OR_n then CER_n.1
            String cerId;
            if (newPhaseId != null) {
                cerId = "CER_phase_"+newPhaseId;
            } else if (namingRound instanceof StockRound_1837) {
                cerId = namingRound.getId().replaceFirst("SR_(\\d+)", "CER_$1.0");
            } else {
                cerId = namingRound.getId().replaceFirst("OR_(\\d+)(\\.\\d+)?", "CER_$1$2");
                if (!cerId.contains(".")) cerId += ".1";
            }
            log.debug("Prev round {}, new round {}", namingRound.getId(), cerId);
            createRound(CoalExchangeRound.class, cerId).start();
        } else {
            doneThisRound.add("CER");
        }
        return runCER;
    }

    /**
     * Check if a national formation (or minor merge) round needs be started
     * @param namingRound The OR in which a phase has changed. Null if we are between rounds.
     * @param interruptedRound The OR in which a phase has changed. Null if we are between rounds.
     */
    public boolean checkAndRunNFR(String newPhaseId, Round namingRound, Round interruptedRound) {
        // Check the nationals for having reached one of their formation steps
        // TODO Can namingRound be removed? Where is the NFR named then?

        this.newPhaseId.set(newPhaseId);
        setInterruptedRound(interruptedRound);
        String[] nationalNames = GameDef_1837.Nationals;
        for (String nationalName : nationalNames) {
            if (doneThisRound.contains(nationalName)) continue;
            PublicCompany_1837 national = (PublicCompany_1837) companyManager.getPublicCompany(nationalName);
            if (phaseManager.hasReachedPhase(national.getFormationStartPhase())
                    && !NationalFormationRound.nationalIsComplete(national)) {
                // Check if this national is affected by a phase change
                if (newPhaseId != null) {
                    if (newPhaseId.equals(national.getFormationStartPhase())
                                && NationalFormationRound.presidencyIsInPool(national)
                            || newPhaseId.equals(national.getForcedStartPhase())
                            || newPhaseId.equals(national.getForcedMergePhase())) {
                        startNationalFormationRound(nationalName);
                        return true;
                    } else {
                        doneThisRound.add(nationalName);
                    }
                } else {
                    startNationalFormationRound(nationalName);
                    return true;
                }
            }
        }
        doneThisRound.clear();

        return false;
    }

    public void startNationalFormationRound(String nationalName) {

        String roundId;
        String nfrReportName;
        if (newPhaseId.value() == null) {
            // After a round
            if (previousSRorOR.value() instanceof OperatingRound_1837) {
                nfrReportName = previousSRorOR.value().getId().replaceFirst(
                        "OR_(\\d+)(\\.\\d+)?", "$1$2");
                if (!nfrReportName.contains(".")) nfrReportName += ".1";
            } else {
                nfrReportName = previousSRorOR.value().getId().replaceFirst(
                        "SR_(\\d+)", "$1.0");
            }
            roundId = "NFR_" + nationalName + "_" + nfrReportName;
        } else {
            // At starting a new phase
            nfrReportName = "phase " + newPhaseId.value();
            roundId = "NFR_" + nationalName + "_phase_" + newPhaseId.value();
        }

        PublicCompany_1837 national = (PublicCompany_1837) companyManager.getPublicCompany(nationalName);
        createRound(NationalFormationRound.class, roundId)
                .start(national, newPhaseId.value() != null, nfrReportName);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.GameManager#runIfStartPacketIsNotCompletelySold()
     */
    @Override
    protected boolean runIfStartPacketIsNotCompletelySold() {
        //After the first Startpacket sold out there will be Operation Rounds
        StartPacket nextStartPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
        return !(nextStartPacket.getId().equalsIgnoreCase("Coal Mines"));
    }

    @Override
    protected void createStartRound(StartPacket startPacket) {
        String startRoundClassName = startPacket.getRoundClassName();
        startRoundNumber.add(1);
        String variant = GameOption.getValue(this, GameOption.VARIANT);
        if (variant.equalsIgnoreCase("1837-2ndEd.")
                && buyOnly.value()) {
            // For subsequent start rounds, we need the buy-only version.
            startRoundClassName += "_buying";
        }
        StartRound startRound = createRound(startRoundClassName,
                "startRound_" + startRoundNumber.value());
        startRound.start();
    }

    @Override
    public void setGuiParameters() {
        super.setGuiParameters();
        guiParameters.put(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME, true);

    }

    public boolean isBuyOnly() {
        return buyOnly.value();
    }

    /**
     * NewPhaseId is the value of the currentPhase, and must be set
     * immediately when a new phase has been reached, insofar such a
     * phase may trigger special rounds that interrupt an OR.
     * Otherwise, when such special rounds run after completing an SR or OR,
     * the value must be null.
     *
     * In 1837, this refers to CoalExchangeRound and NationalFormationRound
     * instances. These can occur both in and between regular rounds.
     * @param newPhaseId String value representing the phase just started, or null.
     */
    public void setNewPhaseId(String newPhaseId) {
        this.newPhaseId.set(newPhaseId);
    }

}
