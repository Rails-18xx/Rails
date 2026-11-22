package net.sf.rails.game.specific._1870;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.StockSpace;

import java.util.ArrayList;
import java.util.List;
import net.sf.rails.game.state.ArrayListState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameManager_1870 extends GameManager {

    private static final Logger log = LoggerFactory.getLogger(GameManager_1870.class);
private final ArrayListState<Round> interruptStack = new ArrayListState<>(this, "interruptStack");

    public GameManager_1870(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * Triggers the 1870-specific Share Protection interaction.
     * When a player sells shares, the President of that company may immediately
     * buy them from the pool to protect the share price from dropping.
     * * @param currentRound The currently active StockRound.
     * 
     * @param company    The company whose shares were just sold.
     * @param sharesSold The number of shares that were sold.
     */
    public void startShareProtectionRound(Round currentRound, PublicCompany company, net.sf.rails.game.Player seller,
            int sharesSold) {

        interruptStack.add((Round) getCurrentRound());
        String roundName = "ShareProtectionRound_in_" + currentRound.getId() + "_" + System.nanoTime();

        ShareProtectionRound_1870 spr = (ShareProtectionRound_1870) createRound(ShareProtectionRound_1870.class,
                roundName);

        // Pass the required context to the new round, now including the seller
        spr.setProtectionContext(company, seller, sharesSold);

        setRound(spr);
        spr.start();

        net.sf.rails.common.ReportBuffer.add(spr,
                "=> INTERRUPT: " + seller.getName() + " sold " + sharesSold + " share(s) of " + company.getId()
                        + ". President " + company.getPresident().getName() + " may protect the price.");
    }

    @Override
    protected void setGuiParameters() {
        super.setGuiParameters();

        // Dynamically register the Cattle Company modifier for revenue calculation
        if (getRoot().getRevenueManager() != null) {
            getRoot().getRevenueManager().addStaticModifier(new net.sf.rails.game.specific._1870.CattleModifier_1870());
            getRoot().getRevenueManager().addStaticModifier(new net.sf.rails.game.specific._1870.GulfModifier_1870());
            getRoot().getRevenueManager()
                    .addDynamicModifier(new net.sf.rails.game.specific._1870.DestinationModifier_1870());

        }
    }

    @Override
    public void nextRound(net.sf.rails.game.Round round) {
        if (round instanceof net.sf.rails.game.specific._1870.ShareProtectionRound_1870 ||
                round instanceof net.sf.rails.game.specific._1870.ConnectionRunRound_1870) {
            if (!interruptStack.isEmpty()) {
                net.sf.rails.game.Round interrupted = interruptStack.remove(interruptStack.size() - 1);
                setRound(interrupted);
                interrupted.resume();
                return;
            }
        }
        super.nextRound(round);
    }

    @Override
    public List<PublicCompany> getCompaniesInDisplayOrder(List<PublicCompany> companies) {
        List<PublicCompany> displayOrder = new ArrayList<>(companies);

        displayOrder.sort((c1, c2) -> {
            // A company is considered a "minor" (or special company) if it does not have a
            // standard stock price,
            // or if its type explicitly marks it as such (Minor, Coal, Destination, Cattle,
            // Gulf).
            boolean isMinor1 = !c1.hasStockPrice() || (c1.getType() != null &&
                    ("Minor".equalsIgnoreCase(c1.getType().getId()) ||
                            "Coal".equalsIgnoreCase(c1.getType().getId()) ||
                            "Destination".equalsIgnoreCase(c1.getType().getId()) ||
                            "Cattle".equalsIgnoreCase(c1.getType().getId()) ||
                            "Gulf".equalsIgnoreCase(c1.getType().getId())));

            boolean isMinor2 = !c2.hasStockPrice() || (c2.getType() != null &&
                    ("Minor".equalsIgnoreCase(c2.getType().getId()) ||
                            "Coal".equalsIgnoreCase(c2.getType().getId()) ||
                            "Destination".equalsIgnoreCase(c2.getType().getId()) ||
                            "Cattle".equalsIgnoreCase(c2.getType().getId()) ||
                            "Gulf".equalsIgnoreCase(c2.getType().getId())));

            // 1. Minors/Special Companies BEFORE Majors
            if (isMinor1 && !isMinor2)
                return -1;
            if (!isMinor1 && isMinor2)
                return 1;

            if (isMinor1 && isMinor2) {
                // Keep minors in their natural ID order
                return c1.getId().compareTo(c2.getId());
            }

            // Both are majors.
            // 2. Order criteria: Open (not closed) before Closed.
            // Cat 1: Open and Started
            // Cat 2: Open and Unstarted (IPO phase)
            // Cat 3: Closed
            int cat1 = c1.isClosed() ? 3 : (c1.hasStarted() ? 1 : 2);
            int cat2 = c2.isClosed() ? 3 : (c2.hasStarted() ? 1 : 2);

            if (cat1 != cat2) {
                return Integer.compare(cat1, cat2);
            }

            // 3. If both are Open and Started (cat 1), sort strictly by stock value
            // (highest first)
            if (cat1 == 1) {
                StockSpace space1 = c1.getCurrentSpace();
                StockSpace space2 = c2.getCurrentSpace();

                if (space1 != null && space2 != null) {
                    // Primary Sort: Price (Highest first)
                    if (space1.getPrice() != space2.getPrice()) {
                        return Integer.compare(space2.getPrice(), space1.getPrice());
                    }
                    // Tie-breakers: column (rightmost first), row (top first), stack position (top
                    // first)
                    if (space1.getColumn() != space2.getColumn()) {
                        return Integer.compare(space2.getColumn(), space1.getColumn());
                    }
                    if (space1.getRow() != space2.getRow()) {
                        return Integer.compare(space1.getRow(), space2.getRow());
                    }
                    return Integer.compare(space1.getStackPosition(c1), space2.getStackPosition(c2));
                }
            }

            // Fallback for Unstarted or Closed majors, or if stock spaces are null
            return c1.getId().compareTo(c2.getId());
        });

        return displayOrder;
    }

    public void startConnectionRunRound(Round currentRound, PublicCompany company) {

        interruptStack.add((Round) getCurrentRound());
        String roundName = "ConnectionRunRound_" + company.getId() + "_" + System.nanoTime();

        ConnectionRunRound_1870 crr = (ConnectionRunRound_1870) createRound(ConnectionRunRound_1870.class, roundName);

        crr.setConnectionCompany(company);

        setRound(crr);
        crr.start();

        net.sf.rails.common.ReportBuffer.add(crr,
                "=> INTERRUPT: " + company.getId() + " performs its immediate Connection Run.");
    }

}