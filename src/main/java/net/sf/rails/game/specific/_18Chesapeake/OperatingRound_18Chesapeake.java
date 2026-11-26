package net.sf.rails.game.specific._18Chesapeake;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.PublicCompany; // Added import
import net.sf.rails.game.MapHex;         // Added import
import net.sf.rails.game.special.SpecialBaseTokenLay; // Added import
import net.sf.rails.game.state.ArrayListState;
import rails.game.action.LayTile;
import java.util.List;

public class OperatingRound_18Chesapeake extends OperatingRound {

    /** Hexes on which the operating company has laid a yellow tile this turn */
    private final ArrayListState<MapHex> yellowTileLaysThisTurn =
            new ArrayListState<>(this, "yellowTileLaysThisTurn");

    public OperatingRound_18Chesapeake(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected void initTurn() {
        super.initTurn();
        yellowTileLaysThisTurn.clear();
    }

    @Override
    public boolean layTile(LayTile action) {
        boolean result = super.layTile(action);
        if (result && action.getLaidTile() != null
                && "yellow".equalsIgnoreCase(action.getLaidTile().getColourText())) {
            yellowTileLaysThisTurn.add(action.getChosenHex());
        }
        return result;
    }

    /**
     * The free station of the C&amp;OC may only be placed immediately after the
     * company has laid the tile on that hex, and counts as its token lay.
     */
    private boolean isSpecialTokenLayAvailable(SpecialBaseTokenLay stl) {
        List<MapHex> locations = stl.getLocations();
        return !normalTokenLaidThisTurn.value() && locations != null
                && locations.stream().anyMatch(yellowTileLaysThisTurn::contains);
    }

    @Override
    protected void setSpecialTokenLays() {
        super.setSpecialTokenLays();
        currentSpecialTokenLays.removeIf(lay -> !isSpecialTokenLayAvailable(lay.getSpecialProperty()));

        PublicCompany company = (PublicCompany) operatingCompany.value();
        if (company == null || !company.canUseSpecialProperties() || company.getNumberOfFreeBaseTokens() == 0) {
            return;
        }

        // Also offer the free station if the company cannot pay for a normal one
        for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
            if (!stl.isExtra() && currentNormalTokenLays.isEmpty() && isSpecialTokenLayAvailable(stl)) {
                List<MapHex> locations = stl.getLocations();
                if (locations != null && !locations.isEmpty()) {
                    boolean canLay = false;
                    for (MapHex location : locations) {
                        if (location.hasTokenOfCompany(company)) continue;
                        for (net.sf.rails.game.Stop stop : location.getStops()) {
                            canLay = !location.isBlockedForTokenLays(company, stop);
                        }
                    }
                    if (canLay) {
                        currentSpecialTokenLays.add(new rails.game.action.LayBaseToken(getRoot(), stl));
                    }
                }
            }
        }
    }

    @Override
    protected boolean validateSpecialTileLay(LayTile layTile) {
        return super.validateSpecialTileLay(layTile);
    }
}
