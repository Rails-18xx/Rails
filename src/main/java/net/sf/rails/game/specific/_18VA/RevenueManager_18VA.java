package net.sf.rails.game.specific._18VA;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueManager;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a subclass of RevenueManager, only meant to adapt
 * ultimate revenue calculation to games with non-standard rules.
 * 18VA is an extreme case of such special rules.
 *
 * I believe that this way of calculating revenue obviates
 * the need for providing additional 'prediction values'
 * in dynamic modifiers.
 *
 * Created by Erik Vos 04/2023
 * */
public class RevenueManager_18VA extends RevenueManager {

    private BiMap<Stop, Stop> portCities;
    private MapManager mapManager;

    private static final Logger log = LoggerFactory.getLogger(RevenueManager_18VA.class);

    public RevenueManager_18VA (RailsRoot parent, String id) {

        super(parent, id);
    }

    public void finishConfiguration(RailsRoot parent)
            throws ConfigurationException {
        portCities = HashBiMap.create();
        mapManager = parent.getMapManager();
        Stop port, city;
        for (String cityName : GameDef_18VA.citiesWithPorts.keySet()) {
            city = mapManager.getHex(cityName).getStops().asList().get(0);
            port = mapManager.getHex(GameDef_18VA.citiesWithPorts.get(cityName)).getStops().asList().get(0);
            portCities.put (port, city);
        }
        log.debug("Related cities of ports {}", portCities);
    }

    @Override
    protected Revenue getBaseRevenue(Stop stop, Train train, PublicCompany company) {

        Revenue baseRev = new Revenue (0,0);
        boolean isGoods = train.getCategory().equalsIgnoreCase("goods");
        boolean isTokened = stop.hasTokenOf(company);
        Phase phase = phaseManager.getCurrentPhase();

        switch (stop.getType()) {
            case TOWN:   // 'Mine' in 18VA parlance
                if (isGoods) {
                    baseRev.addNormalRevenue(stop.getValue());
                }
                break;
            case CITY:
                baseRev.addNormalRevenue(stop.getValueForPhase(phase))
                        .multiplyRevenue(train.getCityScoreFactor());
                break;
            case OFFMAP:
                baseRev.addNormalRevenue(stop.getValueForPhase(phase))
                        .multiplyRevenue(train.getCityScoreFactor()); // 4D scores double
                if (isTokened) {
                    baseRev.multiplyRevenue(2);
                }
                break;
            case MINE:   // 'CMD' in 18VA parlance
                if (isGoods) { // CMD must have configured value 20 - does not work??
                    // FIXME: Why does stop.getValue() return 0 rather than the configured 20?
                    baseRev.addNormalRevenue (20 * train.getMajorStops());
                    if (isTokened
                            && !phaseManager.hasReachedPhase("4D")) {
                        // Add treasury revenue as special revenue
                        baseRev.addSpecialRevenue(baseRev.getNormalRevenue());
                    }
                }
                break;
            case PORT:
                Stop relatedCity = portCities.get(stop);
                // No port revenue without a token in the connected city
                if (relatedCity.hasTokenOf(company)) {
                    baseRev.addNormalRevenue(stop.getValueForPhase(phase));
                    if (isGoods) {
                        baseRev.multiplyRevenue (2)
                                .addRevenue(relatedCity.getValue(), 0);
                    }
                }
        }
        return baseRev;
    }

    // Currently not used.
    public Stop getCityOfPort (Stop port) {
        return portCities.get(port);
    }

    protected String prettyPrint(RevenueAdapter revenueAdapter) {

        String prettyPrint = super.prettyPrint (revenueAdapter);

        if (specialRevenue > 0){
            // Remove a redundant newline (source unknown)
            // .replace() does not work (for unknown reasons)
            prettyPrint = prettyPrint.stripTrailing() + "\n";
            int normalRevenue = revenueAdapter.getTotalRevenue() - specialRevenue;
            prettyPrint += LocalText.getText("DivideEarnings", specialRevenue, normalRevenue);
        }
        return prettyPrint;
    }

}


