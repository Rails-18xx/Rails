/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTokenLay.java,v 1.11 2009/10/31 17:08:26 evos Exp $ */
package rails.game.special;

import java.util.List;

import rails.game.*;
import rails.util.Tag;
import rails.util.Util;

public class SpecialTokenLay extends SpecialProperty {
    String locationCodes = null;
    List<MapHex> locations = null;
    boolean extra = false;
    boolean free = false;
    boolean connected = false;
    Class<? extends Token> tokenClass;
    TokenI token = null;
    int numberAvailable = 1;
    int numberUsed = 0;

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
        try {
            tokenClass = Class.forName(tokenClassName).asSubclass(Token.class);
            if (tokenClass == BonusToken.class) {
                BonusToken bToken = (BonusToken) tokenClass.newInstance();
                token = bToken;
                bToken.configureFromXML(tokenLayTag);

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
    }

    @Override
    public void finishConfiguration (GameManagerI gameManager) 
    throws ConfigurationException {

        locations = gameManager.getMapManager().parseLocations(locationCodes);
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

    public TokenI getToken() {
        return token;
    }

    public String getName() {
        return toString();
    }

    @Override
	public String toString() {
        return "SpecialTokenLay comp=" + privateCompany.getName() + " type="
               + tokenClass.getSimpleName() + ": "
               + (token != null ? token.toString() : "") + " hex="
               + locationCodes + " extra=" + extra + " cost=" + free;
    }
}
