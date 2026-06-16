package net.sf.rails.game.specific._1817.action;

import java.awt.Color;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Station;
import net.sf.rails.game.state.Owner;
import rails.game.action.GuiTargetedAction;
import rails.game.action.LayBaseToken;

public class LayNYHomeToken extends LayBaseToken implements GuiTargetedAction {

    private static final long serialVersionUID = 1L;
    private final String label;

    public LayNYHomeToken(MapHex hex, PublicCompany company, Stop stop) {
        super(hex.getRoot(), hex);
        setCompany(company);
        setChosenStation(stop.getRelatedStationNumber());
        setType(LayBaseToken.HOME_CITY);
        
        Station station = hex.getStation(stop.getRelatedStationNumber());
        // Label will show "North" or "South" based on the station's connection string
    

// Station 1 (city2) is the North station; Station 0 (city1) is the South station.
        String locName = (station.getNumber() == 1) ? "South" : (station.getNumber() == 0) ? "North" : "Station " + station.getNumber();
        this.label = String.format("Place %s Home Station (%s)", 
                                   company.getId(),
                                   locName);
    
    }

    @Override
    public Owner getActor() {
        return getCompany();
    }

    @Override
    public String getGroupLabel() {
        return "IPO Setup";
    }

    @Override
    public String getButtonLabel() {
        return label;
    }

    @Override
    public Color getButtonColor() {
        return new Color(173, 216, 230); // Light Blue for 1817
    }

    @Override
    protected boolean equalsAs(rails.game.action.PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (pa instanceof LayBaseToken) {
            LayBaseToken other = (LayBaseToken) pa;
            return other.getType() == LayBaseToken.HOME_CITY && 
                   "E22".equals(other.getChosenHex().getId()) &&
                   other.getChosenStation() == this.getChosenStation();
        }
        return super.equalsAs(pa, asOption);
    }
}