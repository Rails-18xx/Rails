package net.sf.rails.game.specific._18EU;

import com.google.common.collect.Lists;
import net.sf.rails.algorithms.*;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.specific._18Scan.DestinationRound_18Scan;
import net.sf.rails.game.state.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * In the AlpineToken variant, a train passing though an alpine token
 * may add £10 for each city and off-map area passed.
 */
public class AlpineTokenRevenueModifier implements RevenueDynamicModifier {

    private static final Logger log = LoggerFactory.getLogger(AlpineTokenRevenueModifier.class);

    private PublicCompany company;
    private List<MapHex> alpineTokenHexes = new ArrayList<>();
    private List<MapHex> citiesFound = new ArrayList<>(); // To avoid double counting
    private int alpineTokenBonus = 0;
    private int prediction;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {

        company = revenueAdapter.getCompany();
        if (company.getRoot().getGameOptions().get("AlpineTokens").equalsIgnoreCase("No")) {
            return false;
        }
        alpineTokenHexes.clear();

        // 1. check if the company has any alpine tokens
        for (BaseToken token : company.getAllBaseTokens()) {
            Owner owner = token.getOwner();
            //log.info(">>>>> Company={} token={} owner={} class={}", company, token, owner, owner.getClass());
            if (owner instanceof Stop) {
                MapHex hex = ((Stop) owner).getParent();
                if (hex.getLabel().equalsIgnoreCase("M")) {
                    alpineTokenHexes.add(hex);
                    //log.info("***** {} has an Alpine token on {}", company, hex);
                }
            }
        }
        // 2. Remember the alpine tokened hexes
        prediction = alpineTokenHexes.size() * 40; // Just a blind bet
        return !alpineTokenHexes.isEmpty();
    }

    @Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        MapHex hex;
        int numberOfCities = 0;
        boolean runHasAlpineToken;

        for (RevenueTrainRun run : runs) {
            runHasAlpineToken = false;
            int citiesPerRun = 0;
            citiesFound.clear();

            //log.info("+++++ Checking run of train {} (optimal={})", run.getTrain(), optimalRuns);
            for (NetworkVertex vertex : run.getRunVertices()) {
                hex = vertex.getHex();
                if (hex == null) {
                    log.info ("????? Hex = null");
                }
                if (alpineTokenHexes.contains(hex)) {
                    runHasAlpineToken = true;
                    //log.info ("+++++ {} has an Alpine token on hex {}, tile={}", company, hex, hex.getCurrentTile());
                } else if (hex.getStops() != null && !hex.getStops().isEmpty()) {
                    Stop firstStop = hex.getStops().asList().get(0);
                    String stopType = firstStop.getRelatedStation().getType().toString();
                    if (stopType.matches("CITY|OFFMAP")) {
                        if (!citiesFound.contains(hex)) {
                            citiesPerRun++;
                            citiesFound.add(hex);
                            //log.info("----- {} train {} counts a city at {} stop type {}", company, run.getTrain(), hex, stopType);
                        }
                    } else {
                        //log.info ("..... {} train {} finds no city at {} stop type {}", company, run.getTrain(), hex, stopType);
                    }
                } else {
                    //log.info ("????? Train sees no stops on hex {}, tile {}", hex, hex.getCurrentTile());
                }
            }
            if (runHasAlpineToken) numberOfCities += citiesPerRun;
        }
        alpineTokenBonus = numberOfCities * 10;
        //log.info ("===== {} gets a Alpine bonus of {}", company, alpineTokenBonus);
        return alpineTokenBonus;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        return prediction;
    }

    public boolean providesOwnCalculateRevenue() {
        // does not
        return false;
    }

    public int calculateRevenue(RevenueAdapter revenueAdapter) {
        // zero does no change
        return 0;
    }
    @Override
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // do nothing here (all is done by changing the evaluation value)
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return LocalText.getText("AlpineTokenBonus", alpineTokenBonus,
                citiesFound.toString().replaceAll("[\\[\\]]", ""));
    }

}
