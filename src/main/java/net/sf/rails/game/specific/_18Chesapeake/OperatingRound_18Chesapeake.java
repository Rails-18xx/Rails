package net.sf.rails.game.specific._18Chesapeake;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.PublicCompany; // Added import
import net.sf.rails.game.MapHex;         // Added import
import net.sf.rails.game.special.SpecialBaseTokenLay; // Added import
import rails.game.action.LayTile;
import java.util.List;

public class OperatingRound_18Chesapeake extends OperatingRound {

    public OperatingRound_18Chesapeake(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected void setSpecialTokenLays() {
        super.setSpecialTokenLays();

        PublicCompany company = (PublicCompany) operatingCompany.value();
        if (company == null || !company.canUseSpecialProperties() || company.getNumberOfFreeBaseTokens() == 0) {
            return;
        }

        for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
            if (!stl.isExtra() && currentNormalTokenLays.isEmpty()) {
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