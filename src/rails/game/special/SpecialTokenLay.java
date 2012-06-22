package rails.game.special;

import java.util.List;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.game.state.Item;
import rails.util.*;

public class SpecialTokenLay extends SpecialProperty {
    String locationCodes = null;
    List<MapHex> locations = null;
    boolean extra = false;
    boolean free = false;
    boolean connected = false;
    Class<? extends Token> tokenClass;
    Token token = null;
    int numberAvailable = 1;
    int numberUsed = 0;

    private SpecialTokenLay(Item parent, String id) {
        super(parent, id);
    }

    public static SpecialTokenLay create(Item parent) {
        String uniqueId = SpecialProperty.createUniqueId();
        return new SpecialTokenLay(parent, uniqueId);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag tokenLayTag = tag.getChild("SpecialTokenLay");
        if (tokenLayTag == null) {
            throw new ConfigurationException("<SpecialTokenLay> tag missing");
        }

        locationCodes = tokenLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException(
                    "SpecialTokenLay: location missing");

        extra = tokenLayTag.getAttributeAsBoolean("extra", extra);
        free = tokenLayTag.getAttributeAsBoolean("free", free);
        connected = tokenLayTag.getAttributeAsBoolean("connected", connected);
        closingValue =
                tokenLayTag.getAttributeAsInteger("closingValue", closingValue);

        String tokenClassName =
                tokenLayTag.getAttributeAsString("class",
                        "rails.game.BaseToken");

        String tokenName = "";
        int tokenValue = 0;

        try {
            tokenClass = Class.forName(tokenClassName).asSubclass(Token.class);
            if (tokenClass == BonusToken.class) {
                BonusToken bToken = (BonusToken) tokenClass.newInstance();
                token = bToken;
                bToken.configureFromXML(tokenLayTag);

                tokenName = bToken.getId();
                tokenValue = bToken.getValue();
                numberAvailable =
                        tokenLayTag.getAttributeAsInteger("number",
                                numberAvailable);
            }
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unknown class " + tokenClassName,
                    e);
        } catch (Exception e) {
            throw new ConfigurationException("Cannot instantiate class "
                                             + tokenClassName, e);
        }

        if (tokenClass == BaseToken.class) {
            description = LocalText.getText("LayBaseTokennfo",
                    locationCodes,
                    (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                    (free ? LocalText.getText("noCost") : LocalText.getText("normalCost")));
        } else if (tokenClass == BonusToken.class) {
            description = LocalText.getText("LayBonusTokennfo",
                    tokenName,
                    Bank.format(tokenValue),
                    locationCodes);
        }
    }

    @Override
    public void finishConfiguration (GameManager gameManager)
    throws ConfigurationException {

        locations = gameManager.getMapManager().parseLocations(locationCodes);

        if (token instanceof BonusToken) {
            ((BonusToken)token).prepareForRemoval(gameManager.getPhaseManager());
        }
    }

    public boolean isExecutionable() {
        return true;
    }

    public int getNumberLeft() {
        return numberAvailable - numberUsed;
    }

    public boolean isExtra() {
        return extra;
    }

    public boolean isFree() {
        return free;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationCodeString() {
        return locationCodes;
    }

    public Class<? extends Token> getTokenClass() {
        return tokenClass;
    }

    public Token getToken() {
        return token;
    }

    public String getId() {
        return toString();
    }

    @Override
    public String toString() {
        return "SpecialTokenLay comp=" + originalCompany.getId() + " type="
               + tokenClass.getSimpleName() + ": "
               + (token != null ? token.toString() : "") + " hex="
               + locationCodes + " extra=" + extra + " cost=" + free;
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
