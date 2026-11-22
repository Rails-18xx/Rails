package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.*;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.DisplayBuffer;

import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.DiscardTrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NationalFormationRound extends Round {
    private static final Logger log = LoggerFactory.getLogger(NationalFormationRound.class);

    protected final StringState nationalId = StringState.create(this, "nationalId");
    protected final BooleanState forcedStart = new BooleanState(this, "forcedStart");
    protected final ArrayListState<String> sortedMinorIds = new ArrayListState<>(this, "sortedMinorIds");

    // Tracks which minor we are currently asking (0=K1, 1=K2, 2=K3)
    protected final IntegerState minorIndex = IntegerState.create(this, "MinorIndex", 0);
    protected final StringState lockedMinorOrder = StringState.create(this, "lockedMinorOrder");

    // Helper to track if we need to discard trains at the end
    protected final BooleanState discardStep = new BooleanState(this, "DiscardStep", false);
    // Guard to prevent double-firing of finishRound and corrupting reload states
    protected final BooleanState nfrFinishedGuard = new BooleanState(this, "nfrFinishedGuard", false);
    protected Player currentPlayer;


    protected final net.sf.rails.game.state.HashMapState<String, Integer> directorHistory = net.sf.rails.game.state.HashMapState.create(this, "directorHistory");
    protected final net.sf.rails.game.state.HashMapState<String, Integer> ownerHistory = net.sf.rails.game.state.HashMapState.create(this, "ownerHistory");
    protected final net.sf.rails.game.state.StringState formerMinor1Owner = net.sf.rails.game.state.StringState.create(this, "formerMinor1Owner");

    public NationalFormationRound(GameManager parent, String id) {
        super(parent, id);
    }

    public PublicCompany_1837 getNational() {
        if (nationalId.value() == null)
            return null;
        return (PublicCompany_1837) companyManager.getPublicCompany(nationalId.value());
    }

    // --- STATIC HELPERS REQUIRED BY GameManager_1837 ---
    public static boolean nationalIsComplete(PublicCompany_1837 national) {
        for (PublicCompany company : national.getMinors()) {
            if (!company.isClosed())
                return false;
        }
        return true;
    }

    public static boolean presidencyIsInPool(PublicCompany_1837 national) {
        return national.getPresidentsShare().getOwner() == national.getRoot().getBank().getPool();
    }
    // ---------------------------------------------------

    private List<PublicCompany_1837> getSortedMinors() {
        List<PublicCompany_1837> list = new ArrayList<>();
        PublicCompany_1837 national = getNational();
        if (national == null)
            return list;

        // 1. If we have a locked order, ALWAYS use it to ensure stable clockwise
        // resolution
        String order = lockedMinorOrder.value();
        if (order != null && !order.isEmpty()) {
            for (String id : order.split(",")) {
                PublicCompany c = companyManager.getPublicCompany(id);
                if (c instanceof PublicCompany_1837) {
                    list.add((PublicCompany_1837) c);
                }
            }
            return list;
        }

        // 2. We don't have a locked order. Calculate it dynamically!
        List<PublicCompany_1837> minors = new ArrayList<>();
        for (PublicCompany c : national.getMinors()) {
            if (c instanceof PublicCompany_1837) {
                minors.add((PublicCompany_1837) c);
            }
        }

        Player director = national.getPresident();
        if (director == null) {
            // Unformed: Strict Alphanumeric. DO NOT LOCK (Wait for formation to lock)
            minors.sort(Comparator.comparing(PublicCompany::getId));
            return minors;
        }

        // Formed (Start of new OR): Clockwise from Director
        List<Player> players = gameManager.getPlayers();
        int directorIndex = players.indexOf(director);

        minors.sort((m1, m2) -> {
            Player p1 = m1.getPresident();
            Player p2 = m2.getPresident();

            if (p1 == null && p2 == null)
                return m1.getId().compareTo(m2.getId());
            if (p1 == null)
                return -1;
            if (p2 == null)
                return 1;

            int i1 = players.indexOf(p1);
            int i2 = players.indexOf(p2);

            int dist1 = (i1 >= directorIndex) ? (i1 - directorIndex) : (i1 + players.size() - directorIndex);
            int dist2 = (i2 >= directorIndex) ? (i2 - directorIndex) : (i2 + players.size() - directorIndex);

            if (dist1 == dist2) {
                return m1.getId().compareTo(m2.getId());
            }
            return Integer.compare(dist1, dist2);
        });

        // Lock it NOW so it survives mid-round directorship shifts in this OR
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < minors.size(); i++) {
            sb.append(minors.get(i).getId());
            if (i < minors.size() - 1)
                sb.append(",");
        }
        lockedMinorOrder.set(sb.toString());
        log.info("1837_NFR: Calculated and locked new clockwise evaluation order: " + sb.toString());

        return minors;
    }

    private void lockClockwiseOrder(PublicCompany_1837 national) {
        List<PublicCompany_1837> minors = new ArrayList<>();
        for (PublicCompany c : national.getMinors()) {
            if (c instanceof PublicCompany_1837)
                minors.add((PublicCompany_1837) c);
        }

        Player director = national.getPresident();
        if (director != null) {
            List<Player> players = gameManager.getPlayers();
            int directorIndex = players.indexOf(director);

            minors.sort((m1, m2) -> {
                Player p1 = m1.getPresident();
                Player p2 = m2.getPresident();

                // Closed minors (like the newly formed K1) must go to the front
                // so the advancing minorIndex skips them correctly.
                if (p1 == null && p2 == null)
                    return m1.getId().compareTo(m2.getId());
                if (p1 == null)
                    return -1;
                if (p2 == null)
                    return 1;

                int i1 = players.indexOf(p1);
                int i2 = players.indexOf(p2);

                int dist1 = (i1 >= directorIndex) ? (i1 - directorIndex) : (i1 + players.size() - directorIndex);
                int dist2 = (i2 >= directorIndex) ? (i2 - directorIndex) : (i2 + players.size() - directorIndex);

                if (dist1 == dist2) {
                    return m1.getId().compareTo(m2.getId());
                }
                return Integer.compare(dist1, dist2);
            });
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < minors.size(); i++) {
            sb.append(minors.get(i).getId());
            if (i < minors.size() - 1)
                sb.append(",");
        }
        lockedMinorOrder.set(sb.toString());
        log.info("1837_NFR: Locked clockwise evaluation order: " + sb.toString());
    }

    /**
     * Pure check to see if the national is over the limit.
     */
    private boolean isOverTrainLimit() {
        PublicCompany national = getNational();
        return (national != null && national.hasStarted() &&
                national.getNumberOfTrains() > national.getCurrentTrainLimit());
    }

    @Override
    protected void floatCompany(PublicCompany company) {
        if (!company.hasFloated()) {
            company.setFloated();
        }
    }

    private void setCurrentPlayer(Player p) {
        this.currentPlayer = p;
        playerManager.setCurrentPlayer(p);
    }

    @Override
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    protected void processExchange(PublicCompany minor, PublicCompany major, ExchangeMinorAction action) {
        log.info("1837_NFR: Merging " + minor.getId() + " into " + major.getId());

        if (action.isFormation() && !major.hasFloated()) {
            net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
            net.sf.rails.game.financial.StockSpace parSpace = null;

            boolean isKK = "KK".equals(major.getId());
            int targetPar = isKK ? 120 : 175;

            for (net.sf.rails.game.financial.StockSpace ss : market.getStartSpaces()) {
                if (ss.getPrice() == targetPar) {
                    parSpace = ss;
                    break;
                }
            }
            if (parSpace == null) {
                for (int r = 0; r < 50; r++) {
                    for (int c = 0; c < 50; c++) {
                        net.sf.rails.game.financial.StockSpace ss = market.getStockSpace(r, c);
                        if (ss != null && ss.getPrice() == targetPar) {
                            parSpace = ss;
                            break;
                        }
                    }
                    if (parSpace != null)
                        break;
                }
            }
            if (parSpace != null)
                market.correctStockPrice(major, parSpace);

            // Inject starting capital!
            net.sf.rails.game.state.Currency.fromBank(isKK ? 840 : 875, major);

            major.setFloated();
        }

        Merger1837.mergeMinor(gameManager, minor, major);
    }


    protected void calculateAndLockClockwiseOrder(PublicCompany_1837 national) {
        List<PublicCompany_1837> list = new ArrayList<>();

        // 1. Collect Minors
        for (PublicCompany c : national.getMinors()) {
            if (c instanceof PublicCompany_1837) {
                list.add((PublicCompany_1837) c);
            }
        }

        Player director = national.getPresident();
        if (director == null) {
            // Unformed: Strict Alphanumeric sequence (K1, K2, K3)
            list.sort(Comparator.comparing(PublicCompany::getId));
        } else {
            // Formed: Clockwise from Director
            List<Player> players = gameManager.getPlayers();
            int directorIndex = players.indexOf(director);

            list.sort((m1, m2) -> {
                Player p1 = m1.getPresident();
                Player p2 = m2.getPresident();

                if (p1 == null && p2 == null)
                    return m1.getId().compareTo(m2.getId());
                if (p1 == null)
                    return -1;
                if (p2 == null)
                    return 1;

                int i1 = players.indexOf(p1);
                int i2 = players.indexOf(p2);

                int dist1 = (i1 >= directorIndex) ? (i1 - directorIndex) : (i1 + players.size() - directorIndex);
                int dist2 = (i2 >= directorIndex) ? (i2 - directorIndex) : (i2 + players.size() - directorIndex);

                if (dist1 == dist2)
                    return m1.getId().compareTo(m2.getId());
                return Integer.compare(dist1, dist2);
            });
        }

        // 2. Lock the order in the state variable for the duration of this round
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).getId());
            if (i < list.size() - 1)
                sb.append(",");
        }
        lockedMinorOrder.set(sb.toString());
        log.info("1837_NFR: Locked evaluation order for " + national.getId() + ": " + sb.toString());
    }

    @Override
    public void finishRound() {
        if (nfrFinishedGuard.value()) {
            log.debug("1837_NFR: Suppressing duplicate finishRound() to prevent state corruption.");
            return;
        }
        nfrFinishedGuard.set(true);
        log.info("1837_NFR: finishRound initiated for " + nationalId.value());

        PublicCompany_1837 national = getNational();
        if (national != null && national.hasStarted()) {
            resolveDirectorship(national);
        }

        super.finishRound();
    }


    protected void resolveDirectorship(PublicCompany_1837 major) {
        log.info("1837_NFR: Resolving directorship for " + major.getId());
        java.util.List<Player> players = gameManager.getPlayers();
        Player currentDirector = major.getPresident();
        int maxShares = 0;
        
        java.util.Map<Player, Integer> playerShares = new java.util.HashMap<>();
        for (Player p : players) {
            int shares = 0;
            for (Object obj : p.getPortfolioModel().getCertificates()) {
                if (obj instanceof net.sf.rails.game.financial.PublicCertificate) {
                    net.sf.rails.game.financial.PublicCertificate cert = (net.sf.rails.game.financial.PublicCertificate) obj;
                    if (cert.getCompany().equals(major)) {
                        shares += cert.getShare();
                    }
                }
            }
            playerShares.put(p, shares);
            if (shares > maxShares) maxShares = shares;
        }

        if (maxShares == 0) return;

        java.util.List<Player> tiedPlayers = new java.util.ArrayList<>();
        for (Player p : players) {
            if (playerShares.get(p) == maxShares) {
                tiedPlayers.add(p);
            }
        }

        Player newDirector = currentDirector;

        if (!tiedPlayers.contains(currentDirector) || tiedPlayers.size() > 1) {
            newDirector = tiedPlayers.get(0); 
            if (tiedPlayers.size() > 1) {
                int lowestDirId = 999;
                Player bestDir = null;
                for (Player p : tiedPlayers) {
                    if (directorHistory.containsKey(p.getName()) && directorHistory.get(p.getName()) < lowestDirId) {
                        lowestDirId = directorHistory.get(p.getName());
                        bestDir = p;
                    }
                }

                if (bestDir != null) {
                    newDirector = bestDir;
                } else {
                    int lowestOwnId = 999;
                    Player bestOwn = null;
                    for (Player p : tiedPlayers) {
                        if (ownerHistory.containsKey(p.getName()) && ownerHistory.get(p.getName()) < lowestOwnId) {
                            lowestOwnId = ownerHistory.get(p.getName());
                            bestOwn = p;
                        }
                    }

                    if (bestOwn != null) {
                        newDirector = bestOwn;
                    } else {
                        Player m1Owner = null;
                        for (Player p : players) {
                            if (p.getName().equals(formerMinor1Owner.value())) m1Owner = p;
                        }
                        if (m1Owner != null) {
                            int m1Idx = players.indexOf(m1Owner);
                            int bestDist = 999;
                            for (Player p : tiedPlayers) {
                                int pIdx = players.indexOf(p);
                                int dist = (pIdx >= m1Idx) ? (pIdx - m1Idx) : (pIdx + players.size() - m1Idx);
                                if (dist < bestDist) {
                                    bestDist = dist;
                                    newDirector = p;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (newDirector != null && newDirector != currentDirector) {
            log.info("1837_NFR: Directorship shifts to " + newDirector.getName());
            net.sf.rails.game.financial.PublicCertificate dirCert = null;
            for (net.sf.rails.game.financial.PublicCertificate pc : major.getCertificates()) {
                if (pc.isPresidentShare()) {
                    dirCert = pc;
                    break;
                }
            }
            if (dirCert != null && dirCert.getOwner() instanceof net.sf.rails.game.model.PortfolioOwner) {
                net.sf.rails.game.model.PortfolioOwner oldOwner = (net.sf.rails.game.model.PortfolioOwner) dirCert.getOwner();
                
                int sharesToSwap = dirCert.getShare();
                int swapped = 0;
                java.util.List<net.sf.rails.game.financial.PublicCertificate> certsToMove = new java.util.ArrayList<>();
                for (Object obj : new java.util.ArrayList<>(newDirector.getPortfolioModel().getCertificates())) {
                    if (obj instanceof net.sf.rails.game.financial.PublicCertificate) {
                        net.sf.rails.game.financial.PublicCertificate pc = (net.sf.rails.game.financial.PublicCertificate) obj;
                        if (pc.getCompany().equals(major) && !pc.isPresidentShare()) {
                            certsToMove.add(pc);
                            swapped += pc.getShare();
                            if (swapped >= sharesToSwap) break;
                        }
                    }
                }

                dirCert.moveTo(newDirector);
                for (net.sf.rails.game.financial.PublicCertificate pc : certsToMove) {
                    pc.moveTo(oldOwner);
                }
            }
        }
    }

    
    private void advanceToNextValidMinorOrFinish() {
        List<PublicCompany_1837> minors = getSortedMinors();
        while (minorIndex.value() < minors.size()) {
            PublicCompany_1837 target = minors.get(minorIndex.value());

            if (target != null && !target.isClosed() && target.getPresident() != null) {
                // Auto-process mandatory formations (Phase 5/4+1) without prompting the user.
                if (forcedStart.value()) {
                    log.info("1837_NFR: Auto-processing forced exchange for " + target.getId());
                    PublicCompany_1837 national = getNational();
                    boolean isFormation = (minorIndex.value() == 0 && !national.hasStarted());

                    ExchangeMinorAction ema = new ExchangeMinorAction(target, national, isFormation);

                    if (isFormation) {
                        national.start();
                        executeFormationFloat(national);
                        String msg = LocalText.getText("START_MERGED_COMPANY", national.getId(),
                                Bank.format(this, national.getIPOPrice()), national.getStartSpace());
                        ReportBuffer.add(this, msg);
                        // DisplayBuffer.add(this, msg);
                    }

                    processExchange(target, national, ema);
                    minorIndex.add(1);
                    continue;
                }

                // If not forced, pause here for user input
                return;
            }

            // Skip closed or invalid minor
            minorIndex.add(1);
        }

        // We have processed all minors. Check for discard or finish.
        if (isOverTrainLimit()) {
            discardStep.set(true);
            setCurrentPlayer(getNational().getPresident());
            return;
        }

        finishRound();
    }

    @Override
    public boolean setPossibleActions() {
        possibleActions.clear();
        PublicCompany_1837 national = getNational();

        // 1. DISCARD STEP
        if (discardStep.value()) {
            if (isOverTrainLimit()) {
                generateGroupedDiscardActions(national, possibleActions);
                return true;
            }
            return false;
        }

        // 2. EXCHANGE STEPS
        List<PublicCompany_1837> minors = getSortedMinors();
        if (minorIndex.value() < minors.size()) {
            PublicCompany_1837 target = minors.get(minorIndex.value());
            Player owner = target.getPresident();

            setCurrentPlayer(owner);

            boolean isFormation = (minorIndex.value() == 0 && !national.hasStarted());
            ExchangeMinorAction exchange = new ExchangeMinorAction(target, national, isFormation);

            if (isFormation) {
                exchange.setButtonLabel(LocalText.getText("FormCompany", national.getId()));
            } else {
                exchange.setButtonLabel(LocalText.getText("ExchangeMinorForShare", target.getId(), national.getId()));
            }
            possibleActions.add(exchange);
            if (!forcedStart.value()) {
                NullAction done = new NullAction(getRoot(), NullAction.Mode.DONE);
               // We keep the label for the UI, but we'll add a secondary 
                // unlabeled action if the engine is being strict about matching.
                if (isFormation) {
                    done.setLabel(LocalText.getText("DeclineFormation"));
                    done.setButtonLabel("Decline Formation");
                } else {
                    done.setLabel(LocalText.getText("KeepMinor", target.getId()));
                    done.setButtonLabel("No / Keep Minor");
                }
                possibleActions.add(done);

                // Add a hidden/unlabeled NullAction to catch the UI request 
                // that is failing validation due to the missing label.
                NullAction plainDone = new NullAction(getRoot(), NullAction.Mode.DONE);
                if (isFormation) {
                    plainDone.setButtonLabel("Decline Formation");
                } else {
                    plainDone.setButtonLabel("No / Keep Minor");
                }
                possibleActions.add(plainDone);
            }
            return true;
        }

        // 3. AUTO-CLOSE
        return false;
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof ExchangeMinorAction) {
            ExchangeMinorAction ema = (ExchangeMinorAction) action;
            PublicCompany_1837 national = getNational();
            boolean wasFormation = ema.isFormation();

            if (wasFormation) {
                national.start();
                executeFormationFloat(national);
                String msg = LocalText.getText("START_MERGED_COMPANY", national.getId(),
                        Bank.format(this, national.getIPOPrice()), national.getStartSpace());
                ReportBuffer.add(this, msg);
            }

            processExchange(ema.getMinor(), national, ema);

            if (wasFormation) {
                // Lock the clockwise order NOW, using the formally established national
                // director.
                lockClockwiseOrder(national);
            }

            minorIndex.add(1);
            advanceToNextValidMinorOrFinish();
            return true;
        }

        if (action instanceof NullAction) {

            // Register decline to prevent re-prompting in the same OR/SR for ANY minor
            Object interrupted = gameManager.getInterruptedRound();
            if (interrupted instanceof OperatingRound_1837) {
                ((OperatingRound_1837) interrupted).setNationalFormationDeclined(nationalId.value());
            } else if (interrupted instanceof StockRound_1837) {
                ((StockRound_1837) interrupted).setNationalFormationDeclined(nationalId.value());
            }

            // Case A: Director Declined Formation
            if (minorIndex.value() == 0 && !getNational().hasStarted()) {
                log.info("1837_NFR: Formation DECLINED by " + currentPlayer.getName());
                minorIndex.set(getSortedMinors().size());
                finishRound();
                return true;
            }

            // Case B: Player Kept Minor
            log.info("1837_NFR: Player passed/done on minor " + getSortedMinors().get(minorIndex.value()).getId());
            minorIndex.add(1);
            advanceToNextValidMinorOrFinish();
            return true;
        }

        if (action instanceof DiscardTrain) {
            DiscardTrain dt = (DiscardTrain) action;
            executeDiscardTrain(dt);
            if (!isOverTrainLimit()) {
                finishRound();
            }
            return true;
        }

        return false;
    }

public void start(PublicCompany_1837 national, boolean isTriggered, String reportName) {
        this.nationalId.set(national.getId());
        this.minorIndex.set(0);
        this.discardStep.set(false);

      if (directorHistory.isEmpty()) {
            for (PublicCompany minor : national.getMinors()) {
                if (minor.isClosed()) continue;
                String minorId = minor.getId();
                int minorNum = -1;
                try {
                    minorNum = Integer.parseInt(minorId.replaceAll("\\D", ""));
                } catch (Exception e) { continue; }

                Player dir = minor.getPresident();
                if (dir != null) {
                    if (!directorHistory.containsKey(dir.getName()) || directorHistory.get(dir.getName()) > minorNum) {
                        directorHistory.put(dir.getName(), minorNum);
                    }
                    if (minorNum == 1) formerMinor1Owner.set(dir.getName());
                }

                for (Player p : gameManager.getPlayers()) {
                    if (p instanceof net.sf.rails.game.model.PortfolioOwner) {
                        net.sf.rails.game.model.PortfolioModel pm = ((net.sf.rails.game.model.PortfolioOwner) p).getPortfolioModel();
                        for (Object obj : pm.getCertificates()) {
                            if (obj instanceof net.sf.rails.game.financial.PublicCertificate) {
                                net.sf.rails.game.financial.PublicCertificate cert = (net.sf.rails.game.financial.PublicCertificate) obj;
                                if (cert.getCompany().equals(minor)) {
                                    if (!ownerHistory.containsKey(p.getName()) || ownerHistory.get(p.getName()) > minorNum) {
                                        ownerHistory.put(p.getName(), minorNum);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        boolean isForced = false;
        if ("KK".equals(national.getId())) {
            isForced = getRoot().getBank().getPool().getPortfolioModel().getTrainList().stream()
                    .anyMatch(t -> t.getType().getName().equals("4+1") || t.getType().getName().equals("5"));
            if (!isForced) {
                isForced = getRoot().getCompanyManager().getAllPublicCompanies().stream()
                        .flatMap(c -> c.getPortfolioModel().getTrainList().stream())
                        .anyMatch(t -> t.getType().getName().equals("4+1") || t.getType().getName().equals("5"));
            }
        } else if ("Ug".equals(national.getId())) {
            isForced = getRoot().getBank().getPool().getPortfolioModel().getTrainList().stream()
                    .anyMatch(t -> t.getType().getName().equals("5"));
            if (!isForced) {
                isForced = getRoot().getCompanyManager().getAllPublicCompanies().stream()
                        .flatMap(c -> c.getPortfolioModel().getTrainList().stream())
                        .anyMatch(t -> t.getType().getName().equals("5"));
            }
        }

        this.forcedStart.set(isForced);
        
        
        // 3. Lock the Order before starting
        calculateAndLockClockwiseOrder(national);
        
        ReportBuffer.add(this, LocalText.getText("StartFormationRound", national.getId(), reportName));
        advanceToNextValidMinorOrFinish();
    }



    private void executeFormationFloat(PublicCompany_1837 major) {
        if (!major.hasFloated()) {
            net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
            net.sf.rails.game.financial.StockSpace parSpace = null;

          int targetPar = 175; // Default for Ug
            int startingCapital = 875;
            
            if ("KK".equals(major.getId())) {
                targetPar = 120;
                startingCapital = 840;
            } else if ("Sd".equals(major.getId())) {
                targetPar = 142;
                startingCapital = 710;
            }

            log.info("1837_NFR_LOG: {} starting formation | Par: {} | Capital: {}", major.getId(), targetPar, startingCapital);


            for (net.sf.rails.game.financial.StockSpace ss : market.getStartSpaces()) {
                if (ss.getPrice() == targetPar) {
                    parSpace = ss;
                    break;
                }
            }
            if (parSpace == null) {
                for (int r = 0; r < 50; r++) {
                    for (int c = 0; c < 50; c++) {
                        net.sf.rails.game.financial.StockSpace ss = market.getStockSpace(r, c);
                        if (ss != null && ss.getPrice() == targetPar) {
                            parSpace = ss;
                            break;
                        }
                    }
                    if (parSpace != null)
                        break;
                }
            }
            if (parSpace != null)
                market.correctStockPrice(major, parSpace);

            // Inject starting capital!
net.sf.rails.game.state.Currency.fromBank(startingCapital, major);
            major.setFloated();
            log.info("1837_NFR: " + major.getId() + " successfully floated with Par " + targetPar + " and received capital.");
        }
    }




}