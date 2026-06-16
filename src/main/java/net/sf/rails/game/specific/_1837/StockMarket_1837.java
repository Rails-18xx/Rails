package net.sf.rails.game.specific._1837;

import com.google.common.collect.Lists;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.Owner;

import java.util.List;

public class StockMarket_1837 extends StockMarket {

    private HashMapState<StockSpace, Integer> parSpaceUsers = HashMapState.create(this, "parSpaceUsers");

    public StockMarket_1837(RailsRoot parent, String id) {
        super(parent, id);
        this.stockChartType = ChartType.HEXAGONAL;
    }

    public List<Integer> getStartPrices() {
        List<Integer> prices = Lists.newArrayList();
        for (StockSpace space : startSpaces) {
            if (canAddParSpaceUser(space)) {
                prices.add(space.getPrice());
            }
        }
        return prices;
    }

    @Override
    public StockSpace getStartSpace(int price) {
        StockSpace space = super.getStartSpace(price);
        return canAddParSpaceUser(space) ? space : null;
    }

    public void addParSpaceUser(StockSpace space) {
        Integer count = parSpaceUsers.get(space);
        parSpaceUsers.put(space, (count == null) ? 1 : count + 1);
    }

    public boolean canAddParSpaceUser(StockSpace space) {
        Integer count = parSpaceUsers.get(space);
        return count == null || count < 2;
    }

    public void payOut(PublicCompany company, boolean split) {
        if (!split) {
            // Rule 10.7.4: 100% payout moves Right or Up-Right
            moveRightOrUp(company);
        } else {
            // Rule 10.7.4: 50% payout moves Down-Right
            moveDownRight(company);
        }
    }

    @Override
    public int spacesDownOnSale(int sharesSold, Owner seller) {
        // Rule 9.1: Every sale moves the price marker diagonally down and left
        return 1;
    }

    @Override
    public void soldOut(PublicCompany company) {
        // Rule 9.5: Rise diagonally up-right if director <= 40%, else up-left
        if (company.getPresident() != null && company.getPresident().getPortfolioModel().getShares(company) > 4) {
            moveUpLeft(company);
        } else {
            moveUpRight(company);
        }
    }

    /* Hexagonal Movement Logic for Even-Row (even-r) Pointy-Topped Grid */

    @Override
    public void payOut(PublicCompany company) {
        moveRightOrUp(company);
    }

    @Override
    public void withhold(PublicCompany company) {
        moveLeft(company);
    }

    public void moveRight(PublicCompany company) {
        moveRightOrUp(company);
    }

    public void moveDown(PublicCompany company) {
        // Rule 9.1: Sales move diagonally down and to the left
        moveDownLeft(company);
    }

    @Override
    public void moveUp(PublicCompany company) {
        // Standard Up redirection to Up-Right for 1837 grid
        moveUpRight(company);
    }

    private StockSpace getSpaceByPrice(int targetPrice) {
        for (int r = 0; r < getNumberOfRows(); r++) {
            for (int c = 0; c < getNumberOfColumns(); c++) {
                StockSpace ss = getStockSpace(r, c);
                if (ss != null && ss.getPrice() == targetPrice) {
                    return ss;
                }
            }
        }
        return null;
    }

    @Override
    public void start(PublicCompany company, StockSpace price) {

        super.start(company, price);
    }

    @Override
    public void finishConfiguration(RailsRoot root) {
        super.finishConfiguration(root);

        // Strip the ghost token at boot time.
        // Executing the state mutation prior to action replay bypasses validator
        // hash desynchronization, while legally triggering UI initial state updates.
        PublicCompany sd = root.getCompanyManager().getPublicCompany("Sd");
        if (sd != null && sd.getStartSpace() != null) {
            try {
                java.lang.reflect.Field field = net.sf.rails.game.financial.StockSpace.class
                        .getDeclaredField("fixedStartPrices");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<PublicCompany> fixedStartPrices = (java.util.List<PublicCompany>) field
                        .get(sd.getStartSpace());
                if (fixedStartPrices != null) {
                    boolean removed = fixedStartPrices.remove(sd);
                    org.slf4j.LoggerFactory.getLogger(StockMarket_1837.class)
                            .info("1837_BOOT_FIX: Erased Sd ghost token from par space: " + removed);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(StockMarket_1837.class)
                        .error("1837_BOOT_FIX: Failed to erase Sd ghost token", e);
            }
        }
    }

    @Override
    public void correctStockPrice(PublicCompany company, StockSpace target) {

        super.correctStockPrice(company, target);
    }

    // StockMarket_1837.java

    private void moveDownLeft(PublicCompany company) {
        StockSpace old = company.getCurrentSpace();
        int r = old.getRow();
        int c = old.getColumn();

        // --- START FIX ---
        // XML Down maps to Real Down-Left
        int nr = r + 1;
        int nc = c;
        StockSpace newsquare = getStockSpace(nr, nc);

        // If Down-Left is blocked, deflect to Down-Right
        if (newsquare == null) {
            nr = r + 1;
            nc = c + 1;
            newsquare = getStockSpace(nr, nc);
        }
        // --- END FIX ---

        if (newsquare != null)
            prepareMove(company, old, newsquare);
    }

    private void moveUpLeft(PublicCompany company) {
        StockSpace old = company.getCurrentSpace();
        int r = old.getRow();
        int c = old.getColumn();

        // --- START FIX ---
        // XML Up-Left maps to Real Up-Left
        int nr = r - 1;
        int nc = c - 1;
        StockSpace newsquare = getStockSpace(nr, nc);

        // If Up-Left is blocked, deflect to Up-Right
        if (newsquare == null) {
            nr = r - 1;
            nc = c;
            newsquare = getStockSpace(nr, nc);
        }
        // --- END FIX ---

        if (newsquare != null)
            prepareMove(company, old, newsquare);
    }

    private void moveUpRight(PublicCompany company) {
        StockSpace old = company.getCurrentSpace();
        int r = old.getRow();
        int c = old.getColumn();

        // --- START FIX ---
        // XML Up maps to Real Up-Right
        int nr = r - 1;
        int nc = c;
        StockSpace newsquare = getStockSpace(nr, nc);

        // If Up-Right is blocked, deflect to Up-Left
        if (newsquare == null) {
            nr = r - 1;
            nc = c - 1;
            newsquare = getStockSpace(nr, nc);
        }
        // --- END FIX ---

        if (newsquare != null)
            prepareMove(company, old, newsquare);
    }

    public void moveLeft(PublicCompany company) {
        StockSpace old = company.getCurrentSpace();
        int r = old.getRow();
        int c = old.getColumn();

        // Primary: Left
        StockSpace newsquare = getStockSpace(r, c - 1);

        // --- START FIX ---
        // If Left is blocked, momentum deflects to Down-Left
        if (newsquare == null) {
            int nr = r + 1;
            int nc = c;
            newsquare = getStockSpace(nr, nc);
        }
        // If Down-Left is also blocked, stay or move Down-Right
        if (newsquare == null) {
            int nr = r + 1;
            int nc = c + 1;
            newsquare = getStockSpace(nr, nc);
        }
        // --- END FIX ---

        if (newsquare != null)
            prepareMove(company, old, newsquare);
    }

    private void moveDownRight(PublicCompany company) {
        StockSpace old = company.getCurrentSpace();
        int r = old.getRow();
        int c = old.getColumn();

        // --- START FIX ---
        // Primary: Down-Right
        int nr = r + 1;
        int nc = c + 1;
        StockSpace newsquare = getStockSpace(nr, nc);

        // If Down-Right is blocked, deflect to Down-Left
        if (newsquare == null) {
            nr = r + 1;
            nc = c;
            newsquare = getStockSpace(nr, nc);
        }
        // --- END FIX ---

        if (newsquare != null)
            prepareMove(company, old, newsquare);
    }

    @Override
    protected void moveRightOrUp(PublicCompany company) {
        StockSpace old = company.getCurrentSpace();
        int r = old.getRow();
        int c = old.getColumn();

        // Primary: Right
        StockSpace newsquare = getStockSpace(r, c + 1);

        // --- START FIX ---
        // If blocked or ledge reached, move Up-Right
        if (newsquare == null || old.isLeftOfLedge()) {
            int nr = r - 1;
            int nc = c;
            newsquare = getStockSpace(nr, nc);
        }

        // If Up-Right is blocked, deflect Up-Left
        if (newsquare == null) {
            int nr = r - 1;
            int nc = c - 1;
            newsquare = getStockSpace(nr, nc);
        }
        // --- END FIX ---

        if (newsquare != null)
            prepareMove(company, old, newsquare);
    }
}