package rails.game.action; // Or net.sf.rails.game.action

/**
 * Defines whether a BuyTrain action has a fixed price (for AI scoring)
 * or a variable price (for a human UI modal).
 */
public enum PriceMode {
    /**
     * Action represents a single, fixed price.
     * Used by the AI.
     */
    FIXED,

    /**
     * Action represents a price range, requiring UI-level
     * intervention (e.g., a modal) to set the final price.
     * Used by Human players.
     */
    VARIABLE
}