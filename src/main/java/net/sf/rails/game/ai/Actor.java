package net.sf.rails.game.ai;

// Imports now use the correct package
import java.util.List;
import net.sf.rails.game.GameManager; // Ensure GameManager is imported if needed elsewhere, though not in this signature
import net.sf.rails.game.PublicCompany;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.TokenLayOption;

/**
 * An interface for any entity that can make a game decision (an "Actor").
 */
public interface Actor {

    /**
     * Chooses the next move for the given context.
     * [FIX] Make sure this signature is exactly matched in AIPlayer.java
     */
     PossibleAction chooseMove(
        PublicCompany operatingCompany,
        PossibleActions possibleActions,
        List<TileLayOption> validTileLays,
        List<TokenLayOption> validTokenLays
     );

}