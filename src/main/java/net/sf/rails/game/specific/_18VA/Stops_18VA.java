package net.sf.rails.game.specific._18VA;

import net.sf.rails.game.*;

import java.util.HashMap;
import java.util.Map;

public class Stops_18VA extends Stops {

    private Map<Stop, Stop> cityPorts = new HashMap<>();

    protected Stops_18VA (RailsRoot root) {
        super (root);

        Stop port, city;
        for (String cityName : GameDef_18VA.citiesWithPorts.keySet()) {
            city = mapManager.getHex(cityName).getStops().asList().get(0);
            port = mapManager.getHex(GameDef_18VA.citiesWithPorts.get(cityName)).getStops().asList().get(0);
            cityPorts.put (city, port);
        }
    }

    @Override
    protected Revenue getRevenue(Stop stop, Train train, PublicCompany company) {
        return super.getRevenue(stop, train, company)
                .addRevenue(getExtraRevenue(stop, train, company));
    }

    @Override
    protected Revenue getExtraRevenue(Stop stop, Train train, PublicCompany company) {
        Revenue extraRev = new Revenue(0, 0);
        boolean isGoods = train.getCategory().equalsIgnoreCase("goods");
        boolean isTokened = stop.hasTokenOf(company);
        Phase phase = phaseManager.getCurrentPhase();
        switch (stop.getType()) {
            case PORT:
                // The port revenue is added to the neighbouring city
                extraRev.addNormalRevenue(-stop.getValue());
                break;
            case MINE: // CMD
                if (isGoods) {
                    int factor = stop.hasTokenOf(company) ? 2 : 1;
                    extraRev.addSpecialRevenue (factor * 20 * train.getMajorStops());
                }
                break;
            case OFFMAP:
                if (stop.hasTokenOf(company)) {
                    extraRev.addNormalRevenue(stop.getValueForPhase(phase));
                }
                break;
            case TOWN: // mine
                if (isGoods) extraRev.addNormalRevenue(stop.getValue());
                break;
            case CITY:
                if (cityPorts.containsKey(stop) && isTokened) {
                    Stop port = cityPorts.get(stop);
                    int portValue = port.getValueForPhase(phase);
                    extraRev.addNormalRevenue(stop.getValue() + portValue * (isGoods ? 2 : 1));
                }
                break;
            default:
        }
        return extraRev;
    }
}
