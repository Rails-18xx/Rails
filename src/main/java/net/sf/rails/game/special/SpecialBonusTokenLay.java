package net.sf.rails.game.special;

import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.util.Util;

public class SpecialBonusTokenLay extends SpecialProperty {

    private String locationCodes = null;
    private List<MapHex> locations = null;

    private BonusToken token;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialBonusTokenLay(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag tokenLayTag = tag.getChild("SpecialBonusTokenLay");
        if (tokenLayTag == null) {
            throw new ConfigurationException("<SpecialBonusTokenLay> tag missing");
        }

        locationCodes = tokenLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException(
                    "SpecialBonusTokenLay: location missing");

        BonusToken bToken = BonusToken.create(getParent());
        token = bToken;
        bToken.configureFromXML(tokenLayTag);

        String tokenName = bToken.getId();
        int tokenValue = bToken.getValue();

        description = LocalText.getText("LayBonusTokenInfo",
                tokenName,
                Bank.format(this, tokenValue),
                locationCodes);
    }

    @Override
    public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        locations = root.getMapManager().parseLocations(locationCodes);

        if (token instanceof BonusToken) {
            ((BonusToken)token).prepareForRemoval(root.getPhaseManager());
        }
    }

    public BonusToken getToken() {
        return token;
    }
    
    public boolean isExecutionable() {
        return true;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationCodeString() {
        return locationCodes;
    }

    @Override
    public String toText() {
        return description;
    }

    @Override
    public String toMenu() {
        return description;
    }

    @Override
    public String getInfo() {
        return description;
    }
}
