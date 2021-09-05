package net.sf.rails.game.specific._1837;

import com.google.common.collect.Lists;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PublicCompany_1837 extends PublicCompany {

    private static final Logger log = LoggerFactory.getLogger(PublicCompany_1837.class);

    // For the national companies
    private PublicCompany_1837 startingMinor;
    private String startingMinorName;
    private List<PublicCompany_1837> minors;
    private String minorNames;
    private String formationStartPhase;
    private String forcedStartPhase;
    private String forcedMergePhase;
    private BooleanState complete;

    public PublicCompany_1837(RailsItem parent, String id) {

        super(parent, id);
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        // For national companies
        Tag formationTag = tag.getChild("Formation");
        if (formationTag != null) {
            minorNames = formationTag.getAttributeAsString("minors");
            startingMinorName = formationTag.getAttributeAsString("startMinor");
            formationStartPhase = formationTag.getAttributeAsString("startPhase");
            forcedStartPhase = formationTag.getAttributeAsString("forcedStartPhase");
            forcedMergePhase = formationTag.getAttributeAsString("forcedMergePhase");
        }

    }

    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        super.finishConfiguration(root);

        if (getType().getId().equalsIgnoreCase("National")) {
            complete = new BooleanState (this, getId() + "_complete");
            complete.set (false);

            minors = new ArrayList<>();
            CompanyManager cmgr = getRoot().getCompanyManager();
            if (Util.hasValue(minorNames)) {
                for (String minorName : minorNames.split(",")) {
                    PublicCompany_1837 minor = (PublicCompany_1837) cmgr.getPublicCompany(minorName);
                    minors.add (minor);
                }
            }
            if (Util.hasValue(startingMinorName)) {
                startingMinor = (PublicCompany_1837) cmgr.getPublicCompany(startingMinorName);
            }
        }
    }

        /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#mayBuyTrainType(net.sf.rails.game.Train)
     */
    @Override
    public boolean mayBuyTrainType(Train train) {
        // Coal trains in 1837 are only allowed to buy/operate G-Trains
        if (this.getType().getId().equals("Coal")){
            return train.getType().getCategory().equalsIgnoreCase("goods");
        }
        return super.mayBuyTrainType(train);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#payout(int)
     * TODO: rewrite using the new price rise configuration framework (EV)
     */
    public void payout(int amount, boolean b) {
        if (amount == 0) return;

        // Move the token
        if (hasStockPrice
                && (!payoutMustExceedPriceToMove
                        || amount >= currentPrice.getPrice().getPrice())) {
           ((StockMarket_1837) getRoot().getStockMarket()).payOut(this, b);
        }

    }
    /* (non-Javadoc)
     * @see rails.game.PublicCompany#isSoldOut()
     */
    @Override
    public boolean isSoldOut() {
        Owner owner;

        for (PublicCertificate cert : certificates.view()) {
                owner = cert.getOwner();
                if ((owner instanceof BankPortfolio || owner == cert.getCompany()) && (!owner.getId().equalsIgnoreCase("unavailable"))) {
                    return false;
                }
            }
            return true;
    }

    /**
     * Get the company price to be used in calculating a player's worth,
     * not only at game end but also during the game, for UI display.
     * Implements a proposal by John David Galt
     * @return Company share price to be used in calculating player worth.
     */
    @Override
    public int getGameEndPrice() {
        String type = getType().getId();
        int price = 0;
        switch (type) {
            case "Coal":
            case "Minor1":
            case "Minor2":
                price = getRelatedPublicCompany().getMarketPrice();
                break;
            case "Major":
            case "National":
                price = getMarketPrice();
        }
        log.debug("$$$ {} price is {}", getId(), price);
        return price;
    }

    public List<PublicCompany_1837> getMinors() {
        return minors;
    }

    public PublicCompany_1837 getStartingMinor() { return startingMinor; }

    public boolean isComplete() {
        return complete.value();
    }

    public void setComplete(boolean complete) {
        this.complete.set(complete);
    }

    public String getFormationStartPhase() {
        return formationStartPhase;
    }

    public String getForcedStartPhase() {
        return forcedStartPhase;
    }

    public String getForcedMergePhase() {
        return forcedMergePhase;
    }
}
