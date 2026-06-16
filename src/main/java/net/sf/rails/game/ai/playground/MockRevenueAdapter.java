package net.sf.rails.game.ai.playground;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.game.Phase;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

/**
 * A mock RevenueAdapter for the AI Playground.
 * It bypasses the complex real game logic and returns
 * predictable, testable revenue values.
 */
public class MockRevenueAdapter extends RevenueAdapter {

// Constructor for the playground
    public MockRevenueAdapter() {
        // The super constructor needs these arguments, 
        // but we pass nulls because our mock methods don't use them.
        super(null, null, null, null);
    }

    /**
     * Mocks the revenue calculation.
     * In a real run, this would calculate revenue for the *current* board state.
     * For our test, we'll return a fixed "base" revenue.
     */
    @Override
    public int calculateRevenue() {
        return 30; // Mock base revenue
    }

    /**
     * This is our *hypothetical* calculation. We'll "pretend" to simulate
     * a new tile lay and return a new revenue.
     * @param newTileId The tile being hypothetically laid.
     * @return A new, hypothetical revenue.
     */
    public int calculateHypotheticalRevenue(String newTileId) {
        // Simple mock logic: revenue is base + tile number
        // e.g., Tile "8" gives 30 + 8 = 38 revenue
        // e.g., Tile "57" gives 30 + 57 = 87 revenue
        try {
            return 30 + Integer.parseInt(newTileId);
        } catch (NumberFormatException e) {
            return 40; // Default for non-numeric tiles
        }
    }
}