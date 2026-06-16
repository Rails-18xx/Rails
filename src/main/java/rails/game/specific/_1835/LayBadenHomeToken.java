// File: LayBadenHomeToken.java
package rails.game.specific._1835;

import java.awt.Color;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Station;
import net.sf.rails.game.state.Owner;
import rails.game.action.GuiTargetedAction;
import rails.game.action.LayBaseToken;

public class LayBadenHomeToken extends LayBaseToken implements GuiTargetedAction {

    private static final long serialVersionUID = 1L;
    private final String label;

    public LayBadenHomeToken(MapHex hex, PublicCompany company, Stop stop) {
        super(hex.getRoot(), hex);
        setCompany(company);
        setChosenStation(stop.getRelatedStationNumber());
        setType(LayBaseToken.HOME_CITY);
        
        Station station = hex.getStation(stop.getRelatedStationNumber());
        this.label = String.format("Place Baden Home Station (%s)", 
                                   hex.getConnectionString(station));
    }

    @Override
    public Owner getActor() {
        return getCompany();
    }

    @Override
    public String getGroupLabel() {
        return "Baden Setup";
    }

    @Override
    public String getButtonLabel() {
        return label;
    }


    @Override
    public Color getButtonColor() {
        return new Color(255, 215, 0); // Gold
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return new Color(255, 215, 0); // Gold
    }

    @Override
    public Color getHighlightBorderColor() {
        return new Color(184, 134, 11); // DarkGoldenRod
    }
    
    @Override
    public Color getHighlightTextColor() {
        return Color.BLACK;


        
    }


    @Override
    protected boolean equalsAs(rails.game.action.PossibleAction pa, boolean asOption) {
        // 1. Identity check
        if (pa == this) return true;
        
        // 2. Cross-class serialization check
        // If the engine reconstructed the action from the XML save file as a generic LayBaseToken,
        // we manually verify the critical attributes match our forced Baden L6 prompt.
        if (pa instanceof LayBaseToken) {
            LayBaseToken other = (LayBaseToken) pa;
            if (other.getType() == LayBaseToken.HOME_CITY && 
                other.getChosenHex() != null && 
                "L6".equals(other.getChosenHex().getId())) {
                return true;
            }
        }
        
        // 3. Fallback to normal superclass check
        return super.equalsAs(pa, asOption);
    }
}