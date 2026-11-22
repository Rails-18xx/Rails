package net.sf.rails.game.specific._1817;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 1817 specific Public Company logic.
 * Handles dynamic share units (50, 20, 10) and loan limits (2, 5, 10).
 * Manages certificate sequestering for 2-share starts via the Bank's
 * Unavailable portfolio.
 */
public class PublicCompany_1817 extends PublicCompany {

    private static final Logger log = LoggerFactory.getLogger(PublicCompany_1817.class);

    protected final IntegerState tokenCapacity;
    protected final IntegerState shareCount;
    protected final IntegerState bondsState;

    public PublicCompany_1817(RailsItem parent, String id) {
        super(parent, id);
        this.shareCount = IntegerState.create(this, "shareCount", 2);
        this.bondsState = IntegerState.create(this, "bondsState", 0);
        this.tokenCapacity = IntegerState.create(this, "tokenCapacity", 8);
    }

    @Override
    public void finishConfiguration(RailsRoot root) throws ConfigurationException {
        super.finishConfiguration(root);
        // Rule 1.2.7: Initialize the 5 required short certificates with unique indices
        for (int i = 1; i <= 5; i++) {
            ShortCertificate sc = new ShortCertificate(this, i);

            // We must use the internal CertificatesModel to find the mutable portfolio
            getPortfolioModel().getCertificatesModel().getPortfolio().add(sc);

            // Now that it's registered with the company, sequester it
            sc.moveTo(root.getBank().getUnavailable());
        }
        // Initial certificate setup for all companies
        adjustCertificates();
    }

    @Override
    public int getNumberOfBonds() {
        return bondsState.value();
    }

    public void setNumberOfBonds(int bonds) {
        bondsState.set(bonds);
    }

    /**
     * Executes a single loan transaction for the company.
     * Increments the bond count, transfers $100 from the Bank,
     * and moves the stock price one space to the left/down.
     */
    public void executeLoan() {
        if (getNumberOfBonds() >= getMaxNumberOfLoans()) {
            log.warn("1817_WARNING: Cannot take loan. " + getId() + " is at maximum capacity.");
            return;
        }

        // 1. Update internal bond count
        setNumberOfBonds(getNumberOfBonds() + 1);

        // 2. Transfer cash from the Bank
        addCashFromBank(100, getRoot().getBank());

        // 3. Move stock price left (Rule 1.2.5)
        net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
        if (market instanceof net.sf.rails.game.specific._1817.StockMarket_1817) {
            ((net.sf.rails.game.specific._1817.StockMarket_1817) market).moveLeftOrDown(this, 1);
        }

        // Note: The global 70-loan limit decrement should be handled by the Bank's
        // BondsModel listening to changes in this company's bondsState.
    }

    /**
     * Transfers cash from the Bank to the Company treasury.
     * Uses Currency state movement to ensure the Undo/Redo stack functions
     * correctly.
     */
    public void addCashFromBank(int amount, net.sf.rails.game.financial.Bank bank) {
        net.sf.rails.game.state.Currency.fromBank(amount, this);
    }

    /**
     * Sequesters certificates not belonging to the current share count.
     * In 1817, a 2-share company only uses the President's certificate (100%).
     */
    public void adjustCertificates() {
        int count = shareCount.value();
        // President cert is always 2 units (shares).
        // 2-share company = 2 units total (0 normal certs).
        // 5-share company = 5 units total (3 normal certs).
        // 10-share company = 10 units total (8 normal certs).
        int targetNormalCerts = count - 2;

        List<PublicCertificate> allNormal = new ArrayList<>();

        // Collect all certificates that aren't the President's share and aren't Short
        // Certificates
        for (PublicCertificate cert : getCertificates()) {
            if (!cert.isPresidentShare() && !(cert instanceof ShortCertificate)) {
                allNormal.add(cert);
            }
        }

        int activeCount = 0;
        Owner unavailable = getRoot().getBank().getUnavailable();

        for (PublicCertificate cert : allNormal) {
            if (activeCount < targetNormalCerts) {
                // Move back to Treasury if it's currently sequestered.
                // The company itself is the Owner when a certificate is in its Treasury.
                if (cert.getOwner() != this) {
                    cert.moveTo(getPortfolioModel());
                }
                activeCount++;
            } else {
                // Sequester into the Bank's Unavailable portfolio.
                if (cert.getOwner() != unavailable) {
                    cert.moveTo(unavailable);
                }
            }
        }
    }

    @Override
    public int getShareUnit() {
        int count = shareCount.value();
        if (count == 0)
            return super.getShareUnit();
        return 100 / count;
    }

    @Override
    public int getMaxNumberOfLoans() {
        return shareCount.value();
    }

    public int getShareCount() {
        return shareCount.value();
    }

    @Override
    public int getCurrentTrainLimit() {
        int xmlLimit = super.getCurrentTrainLimit();
        String phaseId = getRoot().getPhaseManager().getCurrentPhase().getId();

        // log.info("1817_DEBUG: getCurrentTrainLimit() called for " + getId());
        // log.info("1817_DEBUG: Current Phase ID: " + phaseId);
        // log.info("1817_DEBUG: XML Engine parsed limit as: " + xmlLimit);

        // Statutory 1817 Train Limits (Rule section 2)
        int actualLimit = 4; // Default for Phases 2 and 3
        if ("8".equals(phaseId) || "7".equals(phaseId) || "6".equals(phaseId)) {
            actualLimit = 2;
        } else if ("5".equals(phaseId) || "4".equals(phaseId)) {
            actualLimit = 3;
        }

        // if (xmlLimit != actualLimit) {
        // log.warn("1817_WARNING: XML limit (" + xmlLimit + ") mismatch. Enforcing
        // statutory limit: " + actualLimit);
        // }

        return actualLimit;
    }

    public void setShareCount(int count) {
        if (count == 2 || count == 5 || count == 10) {
            this.shareCount.set(count);
            log.info("Company " + getId() + " set to " + count + "-share size.");
            adjustCertificates();

            // HOUSE RULE: Immediately populate OSI with Short Certificates when reaching 5
            // or 10 shares
            if (count > 2) {
                net.sf.rails.game.financial.BankPortfolio osi = net.sf.rails.game.financial.Bank.getOSI(this);
                net.sf.rails.game.financial.BankPortfolio unavailable = getRoot().getBank().getUnavailable();

                // Collect safely to avoid ConcurrentModificationException during moveTo
                List<PublicCertificate> shortsToMove = new ArrayList<>();
                for (PublicCertificate cert : unavailable.getPortfolioModel().getCertificates(this)) {
                    if (cert instanceof ShortCertificate) {
                        shortsToMove.add(cert);
                    }
                }

                for (PublicCertificate cert : shortsToMove) {
                    cert.moveTo(osi);
                }
                if (!shortsToMove.isEmpty()) {
                    log.info("1817 House Rule: " + shortsToMove.size() + " Short Certificates for " + getId()
                            + " moved to OSI.");
                    net.sf.rails.common.ReportBuffer.add(this,
                            "1817 House Rule: " + shortsToMove.size() + " Short Certificates for " + getId()
                                    + " have been issued to the Open Short Interest (OSI).");
                }
            }

            // Set station marker capacity according to 1817 rules.

            // Set station marker capacity according to 1817 rules.
            // 2-share: 1 token, 5-share: 2 tokens, 10-share: 4 tokens.
            int capacity = (count == 2) ? 1 : (count == 5) ? 2 : 4;

            // Rule 1.2.6: Train Station provides an additional station marker.
            for (net.sf.rails.game.PrivateCompany priv : getPortfolioModel().getPrivateCompanies()) {
                if ("STA80".equals(priv.getId())) {
                    capacity += 1;
                    log.info("1817: Train Station (STA80) bonus applied to " + getId());
                }
            }

            this.tokenCapacity.set(capacity);

        }
    }

    public void addTokenCapacity(int amount) {
        this.tokenCapacity.add(amount);
    }

    @Override
    public int getShareUnitsForSharePrice() {
        return 1;
    }

    @Override
    public java.util.Set<net.sf.rails.game.BaseToken> getAllBaseTokens() {
        java.util.Set<net.sf.rails.game.BaseToken> all = super.getAllBaseTokens();
        int limit = tokenCapacity.value();
        if (all.size() <= limit) {
            return all;
        }

        java.util.Set<net.sf.rails.game.BaseToken> restricted = new java.util.TreeSet<>();
        int count = 0;
        for (net.sf.rails.game.BaseToken t : all) {
            if (count < limit) {
                restricted.add(t);
            }
            count++;
        }
        return restricted;
    }

    @Override
    public int getNumberOfBaseTokens() {
        return getAllBaseTokens().size();
    }

    @Override
    public int getNumberOfFreeBaseTokens() {
        return Math.max(0, getNumberOfBaseTokens() - getNumberOfLaidBaseTokens());
    }

    @Override
    public net.sf.rails.game.BaseToken getNextBaseToken() {
        if (getNumberOfFreeBaseTokens() <= 0) {
            return null;
        }
        for (net.sf.rails.game.BaseToken t : getAllBaseTokens()) {
            if (!t.isPlaced()) {
                return t;
            }
        }
        return null;
    }

    /**
     * Places the company in a zombie state for liquidation.
     * We bypass super.setClosed() to ensure trains, cash, tokens, and loans
     * are retained for the M&A auction.
     */
    public void close() {
        getIsClosedModel().set(true);
    }

// --- START FIX ---
    public void resetForReuse() {
        getIsClosedModel().set(false);
        setPresident(null);
        setNumberOfBonds(0);
        if (getCurrentSpace() != null) {
            getCurrentSpace().removeToken(this);
            setCurrentSpace(null);
        }

        // Reinitialise must occur AFTER market token removal
        reinitialise();

        // CRITICAL: We must adjust the share count BEFORE sweeping certificates, 
        // ensuring the newly generated 2-share certificates are moved to the IPO.
        setShareCount(2);

        net.sf.rails.game.financial.BankPortfolio ipo = getRoot().getBank().getIpo();
        net.sf.rails.game.financial.BankPortfolio unavailable = getRoot().getBank().getUnavailable();

        // Sweep ALL certificates to sanitize the market state
        for (net.sf.rails.game.financial.PublicCertificate cert : getCertificates()) {
            if (cert instanceof ShortCertificate) {
                if (cert.getOwner() != unavailable) cert.moveTo(unavailable);
            } else {
                if (cert.getOwner() != ipo) cert.moveTo(ipo);
            }
        }

        for (net.sf.rails.game.BaseToken t : new java.util.ArrayList<>(getLaidBaseTokens())) {
            t.moveTo(this);
        }


if (getCash() > 0) {
            net.sf.rails.game.state.Currency.wire(this, getCash(), getRoot().getBank());
        }
        }
// --- END FIX ---

}