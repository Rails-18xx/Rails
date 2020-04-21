package net.sf.rails.game;

import java.util.HashMap;
import java.util.Map;

import net.sf.rails.game.model.PortfolioModel;

public class PortfolioManager extends RailsManager {

    /**
     * Map relating portfolio names and objects, to enable deserialization.
     * KILL: OBSOLETE since Rails 1.3.1, but still required to enable reading old saved files
     */
    protected final Map<String, PortfolioModel> portfolioMap = new HashMap<>();
    /**
     * Map relating portfolio unique names and objects, to enable deserialization
     */
    protected final Map<String, PortfolioModel> portfolioUniqueNameMap = new HashMap<>();


    protected PortfolioManager(RailsItem parent, String id) {
        super(parent, id);
    }

    public void addPortfolio(PortfolioModel portfolio) {
        portfolioMap.put(portfolio.getName(), portfolio);
        portfolioUniqueNameMap.put(portfolio.getUniqueName(), portfolio);
    }

    public PortfolioModel getPortfolioByName(String name) {
        return portfolioMap.get(name);
    }

    public PortfolioModel getPortfolioByUniqueName(String name) {
        return portfolioUniqueNameMap.get(name);
    }

}
