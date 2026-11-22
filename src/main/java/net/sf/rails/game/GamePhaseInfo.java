package net.sf.rails.game;

import java.io.Serializable;
import java.util.List;

/**
 * GamePhaseInfo models a single row or configuration of an 18xx game phase transition.
 * It serves as a unified data container populated from rules.json, mapping train purchases 
 * to structural game modifications (tile color shifts, train rust triggers, and roster limits).
 * * It also exposes utility methods to query phase logic dynamically, providing structured data 
 * for UI tables, train card buying alerts, and inventory tool-tips.
 */
public class GamePhaseInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String phase;
    public final String onTrain;
    public final int operatingRounds;
    public final int trainLimit;
    public final String colour;
    public final String rustsTrains;
    public final String status;
    public final String effects;

    public GamePhaseInfo(String phase, String onTrain, int operatingRounds, 
                         int trainLimit, String colour, String rustsTrains, 
                         String status, String effects) {
        this.phase = phase;
        this.onTrain = onTrain;
        this.operatingRounds = operatingRounds;
        this.trainLimit = trainLimit;
        this.colour = colour;
        this.rustsTrains = rustsTrains;
        this.status = status;
        this.effects = effects;
    }

    /**
     * STUB 1: Formats and returns a clear 2D Object array structure for Swing JTable models.
     * Maps perfectly to columns: Phase, On Train, ORs, Train Limit, Colour, Status, and Effects.
     * * @param fullPhaseList The complete list of cached phase elements from GameManager.
     * @return A row-by-row matrix suitable for display panels.
     */
    public static Object[][] getPhaseOverviewTable(List<GamePhaseInfo> fullPhaseList) {
        // TODO: Map the list rows into a clear grid array matching the visual screenshot matrix layout.
        return new Object[0][0];
    }

    /**
     * STUB 2: Generates contextual HTML tool-tip descriptions for active train cards in the bank pool.
     * Triggers forward-facing status updates (e.g., "Buying a '4' train will rust all active '2' trains").
     * * @param fullPhaseList The complete list of cached phase elements from GameManager.
     * @param targetTrainName The name/tier of the train card currently hovered or selected (e.g., "4").
     * @return HTML-formatted tool-tip string profiling tile availability and cascading effects.
     */
    public static String getBankTrainCardTooltip(List<GamePhaseInfo> fullPhaseList, String targetTrainName) {
        // TODO: Scan phases where onTrain equals targetTrainName. 
        // Aggregate structural impacts: what it rusts, current tile tier colors, and cap updates.
        return "";
    }

    /**
     * STUB 3: Generates reverse lookup information for an existing train owned by a corporation.
     * Informs players of obsolescence threats (e.g., "This '2' train will be rusted when the '4' train is bought").
     * * @param fullPhaseList The complete list of cached phase elements from GameManager.
     * @param ownedTrainName The name/tier of the train currently in a company portfolio (e.g., "2").
     * @return HTML-formatted warning alert specifying exactly which incoming phase/train will rust this asset.
     */
    public static String getOwnedTrainObsolescenceTooltip(List<GamePhaseInfo> fullPhaseList, String ownedTrainName) {
        // TODO: Scan phases looking for rustsTrains containing ownedTrainName.
        // Return alert details revealing the dangerous phase/train threshold to the player.
        return "";
    }
}