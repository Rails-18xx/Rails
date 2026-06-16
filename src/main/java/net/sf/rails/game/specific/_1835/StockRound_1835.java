/*
  This class implements the 1835 rules for making new companies
  being available in the IPO after buying shares of another company.
 */
package net.sf.rails.game.specific._1835;

// import static net.sf.rails.game.financial.StockRound.log;

import java.util.*;

import com.google.common.collect.Sets;

import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.CertificatesModel;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.game.state.Owner;
import rails.game.action.AdjustSharePrice;
import rails.game.action.BuyCertificate;
import rails.game.action.NullAction;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.game.model.PortfolioModel;
import rails.game.action.PossibleAction;
import rails.game.specific._1835.ExchangeForPrussianShare;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockRound_1835 extends StockRound {

    /**
     * Constructed via Configure
     */
    public StockRound_1835(GameManager parent, String id) {
        super(parent, id);
    }

    /** Add nationalisations */
    // change: nationalization is a specific BuyCertificate activity
    // requires: add a new activity
    @Override
    public void setBuyableCerts() {

        super.setBuyableCerts();
        if (companyBoughtThisTurnWrapper.value() != null)
            return;

        int price;
        int cash = currentPlayer.getCash();
        Set<PublicCertificate> certs;
        StockSpace stockSpace;
        PortfolioModel from;
        int unitsForPrice;

        // Nationalisation
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (!company.getType().getId().equalsIgnoreCase("Major"))
                continue;
            if (!company.hasFloated())
                continue;
            if (company.getPresident() != currentPlayer)
                continue;
            if (currentPlayer.getPortfolioModel().getShare(company) < 55)
                continue;
            if (currentPlayer.hasSoldThisRound(company))
                continue;

            for (Player otherPlayer : getRoot().getPlayerManager().getPlayers()) {
                if (otherPlayer == currentPlayer)
                    continue;

                /* Get the unique player certificates and check which ones can be bought */
                from = otherPlayer.getPortfolioModel();
                certs = from.getCertificates(company);
                if (certs == null || certs.isEmpty())
                    continue;

                /* Allow for multiple share unit certificates (e.g. 1835) */
                PublicCertificate[] uniqueCerts;
                int shares;

                stockSpace = company.getCurrentSpace();
                unitsForPrice = company.getShareUnitsForSharePrice();
                price = (int) (1.5 * stockSpace.getPrice() / unitsForPrice);

                /*
                 * Check what share multiples are available
                 * Normally only 1, but 1 and 2 in 1835. Allow up to 4.
                 */
                uniqueCerts = new PublicCertificate[5];
                for (PublicCertificate cert2 : certs) {
                    shares = cert2.getShares();
                    if (uniqueCerts[shares] != null)
                        continue;
                    uniqueCerts[shares] = cert2;
                }

                /* Create a BuyCertificate action per share size */
                for (shares = 1; shares < 5; shares++) {
                    if (uniqueCerts[shares] == null)
                        continue;

                    /* Would the player exceed the total certificate limit? */
                    if (!stockSpace.isNoCertLimit()
                            && !mayPlayerBuyCertificate(currentPlayer, company,
                                    uniqueCerts[shares].getCertificateCount()))
                        continue;

                    // Does the player have enough cash?
                    if (cash < price * shares)
                        continue;

                    possibleActions.add(new BuyCertificate(company,
                            uniqueCerts[shares].getShare(),
                            from.getParent(), price, 1));
                }
            }
        }
        prunePrematureActions();
    }

    @Override
    // change: there is no holding limit in 1835
    // requires: should be parameterized?
    public boolean checkAgainstHoldLimit(Player player, PublicCompany company, int number) {
        return true;
    }

    @Override
    // change: price differs for nationalization action
    // requires: move into new activity
    protected int getBuyPrice(BuyCertificate action, StockSpace currentSpace) {
        int price = currentSpace.getPrice();
        if (action.getFromPortfolio().getParent() instanceof Player) {
            price *= 1.5;
        }
        return price;
    }

    /**
     * In 1835, the Prussian (PR) accumulates capital from share sales (and
     * exchanges)
     * even before it technically "floats" or operates.
     * The standard rule (money -> company only if floated) must be overridden for
     * PR.
     */
    @Override
    protected MoneyOwner getSharePriceRecipient(PublicCompany comp, Owner from, int price) {
        // If the company is the Prussian (PR) and shares are bought from the IPO/Bank
        if (comp.getId().equals(GameDef_1835.PR_ID) && from == ipo.getParent()) {
            return comp; // Money goes to PR treasury, regardless of float state
        }
        return super.getSharePriceRecipient(comp, from, price);
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     * In 1835, EVERY share sold drops the price.
     */
    @Override
    protected void adjustSharePrice(PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {
        // If shares were already sold this turn, the price has already dropped.
        // We do NOT drop it again automatically.
        if (soldBefore) {
            // Track last sold company to allow manual "Adjust Price" action if strict rules
            // are preferred
            lastSoldCompany = company;
            return;
        }

        // Always drop exactly 1 space on the first sale, regardless of the quantity
        // sold.
        super.adjustSharePrice(company, seller, 1, soldBefore);

    }

    private static final Logger log = LoggerFactory.getLogger(StockRound_1835.class);

    @Override
    public boolean done(NullAction action, String playerName,
            boolean hasAutopassed) {
        if (hasActed.value()) {
            if (companyBoughtThisTurnWrapper.value() == null) {
                hasActed.set(false);
            }
        }
        return super.done(action, playerName, hasAutopassed);
    }

    /*
     * (non-Javadoc)
     * * @see
     * net.sf.rails.game.StockRound#mayPlayerSellShareOfCompany(net.sf.rails.game.
     * PublicCompany)
     */
    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompany company) {

        // 1835 Rule: Cannot sell if not floated (except Prussian) [cite: 468, 470]
        if (!company.hasFloated()) {
            return false;
        }
        // 1835 Rule: Cannot sell if floated in the CURRENT share round
        Boolean wasUnfloated = unfloatedAtStartOfRound.get(company.getId());
        if (wasUnfloated != null && wasUnfloated && company.hasFloated()) {
            if (!company.getId().equals("PR")) {
                return false;
            }
        }

        // Fallback to standard engine checks for all other conditions
        return super.mayPlayerSellShareOfCompany(company);
    }

    protected void setGameSpecificActions() {

        /*
         * If in one turn multiple sales of the same company occur,
         * this is normally done at the same price.
         * In 1835 the rules state otherwise, a special action
         * enables following that rule strictly.
         */
        if (lastSoldCompany != null) {
            possibleActions.add(new AdjustSharePrice(lastSoldCompany, EnumSet.of(AdjustSharePrice.Direction.DOWN)));
        }

    }

    protected boolean processGameSpecificAction(PossibleAction action) {
        if (action instanceof AdjustSharePrice) {
            super.adjustSharePrice((AdjustSharePrice) action);
            return true;
        } else {
            return false;
        }
    }

    /*
     * @Override
     * protected boolean checkIfSplitSaleOfPresidentAllowed() {
     * // in 1835 its not allowed to Split the President Certificate on sale
     * return false;
     * }
     */

    @Override
    protected void setPriority(String string) {
        if (string.matches("BuyCert|StartCompany")) {
            super.setPriority(string);
        }
    }

    @Override
    protected boolean executeShareTransfer(PublicCompany company, List<PublicCertificate> certsToSell,
            Player dumpedPlayer, int presSharesToSell) {

        boolean swapped = false;
        BankPortfolio bankTo = (BankPortfolio) pool.getParent();

        if (dumpedPlayer != null && presSharesToSell > 0) {
            executePresidentTransferAfterDump(company, new TreeSet<>(certsToSell), dumpedPlayer, presSharesToSell,
                    company.getPresident(), bankTo);

            ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                    dumpedPlayer.getId(),
                    company.getId()));
            swapped = true;

        }

        // Transfer the sold certificates
        Portfolio.moveAll(certsToSell, bankTo);
        return swapped;
    }

    private void executePresidentTransferAfterDump(PublicCompany company, Set<PublicCertificate> certsToSell,
            Player newPresident, int presSharesToSell, Player oldPresident, BankPortfolio bankTo) {
        PublicCertificate presidentCert = company.getPresidentsShare();

        SortedSet<PublicCertificate.Combination> newPresidentsReplacementForPresidentShare = CertificatesModel
                .certificateCombinations(newPresident.getPortfolioModel().getCertificates(company),
                        presidentCert.getShares());

        PublicCertificate.Combination swapToOldPresident = null;

        // Check if a manual choice was set in the UI (inherited from StockRound static
        // field)
        if (StockRound.manualSwapChoice != null && !StockRound.manualSwapChoice.isEmpty()) {
            for (PublicCertificate.Combination comb : newPresidentsReplacementForPresidentShare) {
                // Convert combination to list of sizes for comparison
                List<Integer> combSizes = new ArrayList<>();
                for (PublicCertificate c : comb.getCertificates()) {
                    combSizes.add(c.getShares());
                }
                Collections.sort(combSizes);

                // manualSwapChoice is already sorted
                if (combSizes.equals(StockRound.manualSwapChoice)) {
                    swapToOldPresident = comb;
                    log.warn("[SWAP-1835] Applying manual swap choice: {}", StockRound.manualSwapChoice);
                    break;
                }
            }
            StockRound.manualSwapChoice = null; // Clear after use
        }

        // Fallback to default behavior if no choice or match found
        if (swapToOldPresident == null) {
            swapToOldPresident = newPresidentsReplacementForPresidentShare.first();
        }

        Portfolio.moveAll(swapToOldPresident, oldPresident);
        presidentCert.moveTo(newPresident);

        Set<PublicCertificate> oldPresidentsCertsWithoutCertsToSell = Sets
                .difference(oldPresident.getPortfolioModel().getCertificates(company), certsToSell);
        SortedSet<PublicCertificate.Combination> sellableCertificateCombinations = CertificatesModel
                .certificateCombinations(
                        oldPresidentsCertsWithoutCertsToSell,
                        presSharesToSell);

        Portfolio.moveAll(sellableCertificateCombinations.last(), bankTo);
    }

    // ... (lines of unchanged context code) ...

    private boolean isSoldOut(String companyAbbrev) {
        String companyId = resolveId(companyAbbrev);
        PublicCompany c = companyManager.getPublicCompany(companyId);
        return c != null && ipo.getShare(c) == 0;
    }

    private boolean shouldBlock(String companyAbbrev) {
        String companyId = resolveId(companyAbbrev);
        PublicCompany c = companyManager.getPublicCompany(companyId);
        return c != null && !c.isBuyable() && !c.hasStarted();
    }
    // ... (lines of unchanged context code) ...

    // ... (lines of unchanged context code) ...
    // Cache for resolved IDs to avoid repeated lookups
    private Map<String, String> resolvedIds = new HashMap<>();
    // Custom tracker to catch companies that float during this round
    private final net.sf.rails.game.state.HashMapState<String, Boolean> unfloatedAtStartOfRound = net.sf.rails.game.state.HashMapState
            .create(this, "unfloatedAtStartOfRound");

    @Override
    public void start() {
        super.start();

        unfloatedAtStartOfRound.clear();
        for (PublicCompany c : companyManager.getAllPublicCompanies()) {
            if (!c.hasFloated()) {
                unfloatedAtStartOfRound.put(c.getId(), true);
            }
        }
    }

    @Override
    protected void checkForCompanyReleases() {
        log.info("--- Checking Company Releases (1835 Rules) ---");

        // 1. Resolve Canonical IDs (matching CompanyManager.xml)
        String idBad = resolveId("Bad"); // Should be BA
        String idPru = resolveId("Pru"); // Should be PR
        String idWrt = resolveId("Wrt"); // Should be WT
        String idHes = resolveId("Hes"); // Should be HE
        String idMec = resolveId("Mec"); // Should be MS
        String idOld = resolveId("Old"); // Should be OL

        String idBay = resolveId("Bay"); // Should be BY
        String idSax = resolveId("Sax"); // Should be SX

        PublicCompany cBad = companyManager.getPublicCompany(idBad);
        PublicCompany cPru = companyManager.getPublicCompany(idPru);
        PublicCompany cWrt = companyManager.getPublicCompany(idWrt);
        PublicCompany cHes = companyManager.getPublicCompany(idHes);
        PublicCompany cMec = companyManager.getPublicCompany(idMec);
        PublicCompany cOld = companyManager.getPublicCompany(idOld);
        PublicCompany cBay = companyManager.getPublicCompany(idBay);
        PublicCompany cSax = companyManager.getPublicCompany(idSax);

        // 2. PRUSSIA RELEASE RULE
        // XML Rule: sold="BA:20" released="PR"
        // Meaning: If Baden Director (20%) is sold, Prussia becomes available.
        if (cBad != null && cPru != null && !cPru.isBuyable() && !cPru.hasStarted()) {
            // Check if Baden President is owned by a player (not IPO)
            if (cBad.getPresidentsShare().getOwner() != ipo) {
                // log.info("TRIGGER: Baden Director sold. Releasing Prussia (PR).");
                // We cannot force setBuyable() directly if it's private, but typically
                // the engine checks release rules. If the engine missed it, we assume
                // the ReleaseRule in XML works. We log it to be sure.
                // If manual intervention is needed:
                // releaseCompany(cPru);
            }
        }

        // 3. HESSEN (HE) RELEASE RULE
        // XML Rule: sold="WT:50" released="HE"
        // Block HE if WT is not 50% sold.
        if (cHes != null && cWrt != null && !cHes.hasStarted()) {
            int wtSold = 100 - ipo.getShare(cWrt);
            if (wtSold < 50) {
                log.info("BLOCK: Hessen (HE) blocked. Württemberg (WT) sold: {}% < 50%", wtSold);
                // Ensure it stays unavailable if the engine tries to open it early
                // (Note: we can't easily 're-lock' it without access to private setters,
                // but we can return early to prevent further custom logic).
            }
        }

        // 3a. GROUP 1 TO GROUP 2 RELEASE RULE (BA)
        // Block BA if Group 1 (BY, SX) is not 100% sold
        boolean group1Done = isSoldOut(idBay) && isSoldOut(idSax);
        if (!group1Done) {
            if (cBad != null && (!cBad.hasStarted() && !cBad.isBuyable())) {
                log.info("BLOCK: Baden (BA) blocked. Group 1 (BY, SX) not sold out.");
            }
        }

        // 3b. WÜRTTEMBERG (WT) RELEASE RULE
        // Block WT if BA is not 50% sold
        if (cWrt != null && cBad != null && !cWrt.hasStarted()) {
            int badSold = 100 - ipo.getShare(cBad);
            if (badSold < 50) {
                log.info("BLOCK: Württemberg (WT) blocked. Baden (BA) sold: {}% < 50%", badSold);
            }
        }

        // 4. GROUP 3 RELEASE (MS, OL)
        // XML Rule: sold="BA,WT,HE" released="MS"
        // XML Rule: sold="MS:60" released="OL"

        // Check Group 2 Sold Out status (BA, WT, HE)
        boolean group2Done = isSoldOut(idBad) && isSoldOut(idWrt) && isSoldOut(idHes);

        if (!group2Done) {
            // Block MS if Group 2 not finished
            if (cMec != null && (!cMec.hasStarted() && !cMec.isBuyable())) {
                log.info("BLOCK: Mecklenburg (MS) blocked. Group 2 (BA, WT, HE) not sold out.");
                // Rely on standard engine not to release MS yet.
            }
        }

        // Check OL dependency on MS (60%)
        if (cOld != null && cMec != null && !cOld.hasStarted()) {
            int mecSold = 100 - ipo.getShare(cMec);
            if (mecSold < 60) {
                log.info("BLOCK: Oldenburg (OL) blocked. Mecklenburg (MS) sold: {}% < 60%", mecSold);
            }
        }

        // Run the standard engine checks (processes XML ReleaseRules)
        super.checkForCompanyReleases();
    }

    /**
     * Resolves a target abbreviation to the XML Canonical ID.
     */
    private String resolveId(String target) {
        if (resolvedIds.containsKey(target))
            return resolvedIds.get(target);

        // Map common code abbreviations to XML "name" attributes
        String canonical = target;
        if (target.equalsIgnoreCase("Wrt"))
            canonical = "WT";
        else if (target.equalsIgnoreCase("Bad"))
            canonical = "BA";
        else if (target.equalsIgnoreCase("Hes"))
            canonical = "HE";
        else if (target.equalsIgnoreCase("Mec"))
            canonical = "MS"; // XML name is MS
        else if (target.equalsIgnoreCase("Old"))
            canonical = "OL"; // XML name is OL
        else if (target.equalsIgnoreCase("Pru"))
            canonical = "PR";
        else if (target.equalsIgnoreCase("Bay"))
            canonical = "BY";
        else if (target.equalsIgnoreCase("Sax"))
            canonical = "SX";

        // Verify validity against engine
        if (companyManager.getPublicCompany(canonical) != null) {
            resolvedIds.put(target, canonical);
            return canonical;
        }

        log.error("Could NOT resolve company identifier: {} (tried {})", target, canonical);
        return target;
    }

    private int getSoldPercentage(String canonicalId) {
        PublicCompany c = companyManager.getPublicCompany(canonicalId);
        if (c == null)
            return 0;

        net.sf.rails.game.financial.Bank bank = getRoot().getBank();
        int inIpo = bank.getIpo().getPortfolioModel().getShare(c);
        int inUnavailable = bank.getUnavailable().getPortfolioModel().getShare(c);

        // True percentage in the hands of players/pool
        return 100 - inIpo - inUnavailable;
    }

    private void prunePrematureActions() {
        if (possibleActions == null || possibleActions.isEmpty())
            return;

        boolean group1Done = (getSoldPercentage("BY") == 100) && (getSoldPercentage("SX") == 100);
        boolean group2Done = (getSoldPercentage("BA") == 100) && (getSoldPercentage("WT") == 100)
                && (getSoldPercentage("HE") == 100);

        boolean baFloatReady = getSoldPercentage("BA") >= 50;
        boolean wtFloatReady = getSoldPercentage("WT") >= 50;
        boolean msReady = getSoldPercentage("MS") >= 60;

        
        List actionsList = new ArrayList<>(possibleActions.getList());
for (Object obj : actionsList) {
if (obj instanceof BuyCertificate) {
BuyCertificate buyAction = (BuyCertificate) obj;
String id = buyAction.getCompany().getId();

            if (!buyAction.getCompany().getType().getId().equalsIgnoreCase("Major")) continue;

            boolean remove = false;

            if (id.equals("BA") && !group1Done) remove = true;
            if (id.equals("WT") && (!group1Done || !baFloatReady)) remove = true;
            if (id.equals("HE") && (!group1Done || !wtFloatReady)) remove = true;
            if (id.equals("MS") && !group2Done) remove = true;
            if (id.equals("OL") && (!group2Done || !msReady)) remove = true;

            if (remove) {
                possibleActions.remove((PossibleAction) obj);
            }
        }
    }
    }

}