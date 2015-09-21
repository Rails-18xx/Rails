package net.sf.rails.ui.swing.hexmap;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.NavigableSet;
import java.util.Set;

import javax.swing.JLabel;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Stop;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.ui.swing.elements.TokenIcon;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import rails.game.action.LayBaseToken;
import rails.game.action.LayBonusToken;
import rails.game.action.LayToken;

public class TokenHexUpgrade extends HexUpgrade {

    public enum Invalids implements HexUpgrade.Invalids {
        HEX_BLOCKED, HEX_RESERVED, NOT_ENOUGH_CASH, CONTAINS_TOKEN, REQUIRES_TILE, REQUIRES_NO_TILE;

        @Override
        public String toString() {
            return LocalText.getText("TOKEN_UPGRADE_INVALID_" + this.name());
        }
        
    }

    // static fields
    private final LayToken action;
    private final ImmutableSet<Stop> stops;

    // validation fields
    private final NavigableSet<Stop> allowed = Sets.newTreeSet();
    private final EnumSet<Invalids> invalids = EnumSet.noneOf(Invalids.class);

    // ui fields
    private Stop selectedStop;
    
    private TokenHexUpgrade(GUIHex hex, Collection<Stop> stops, LayToken action) {
        super(hex);
        this.action = action;
        this.stops = ImmutableSet.copyOf(stops);
    }
    
    public static TokenHexUpgrade create(GUIHex hex, Collection<Stop> stops, LayToken action) {
        return new TokenHexUpgrade(hex, stops, action);
    }
    
    public LayToken getAction() {
        return action;
    }
    
    public Set<Stop> getStops() {
        return stops;
    }
    
    public Stop getSelectedStop() {
        return selectedStop;
    }

    private boolean validate() {
        invalids.clear();
        allowed.addAll(stops);

        // LayBonusToken always and layHome is always allowed
        if (!(action instanceof LayBonusToken || ((LayBaseToken)action).getType() == LayBaseToken.HOME_CITY)) {
            if (hexBlocked()) {
                invalids.add(Invalids.HEX_BLOCKED);
            }
            if (hexReserved()) {
                invalids.add(Invalids.HEX_RESERVED);
            }
            if (notEnoughCash()) {
                invalids.add(Invalids.NOT_ENOUGH_CASH);
            }
            if (containsToken()) {
                invalids.add(Invalids.CONTAINS_TOKEN);
            }
            if (requiresTile()) {
                invalids.add(Invalids.REQUIRES_TILE);
            }
            if (requiresNoTile()) {
                invalids.add(Invalids.REQUIRES_NO_TILE);
            }
        }

        if (allowed.isEmpty() || !invalids.isEmpty()) {
            selectedStop = null;
            return false;
        } else {
            selectedStop = allowed.first();
            return true;
        }
    }
    
    public boolean hexBlocked() {
        return hex.getHex().getBlockedForTokenLays() == MapHex.BlockedToken.ALWAYS;
    }
    
    public boolean hexReserved() {
        for (Stop stop:stops) {
            if (hex.getHex().isBlockedForReservedHomes(stop)) {
                allowed.remove(stop);
            }
        }
        return allowed.isEmpty();
    }
    
    public boolean notEnoughCash() {
        return action.getCompany().getCash() < this.getCost(); 
    }
    
    public boolean containsToken() {
        return hex.getHex().hasTokenOfCompany(action.getCompany());
    }
    
    public boolean requiresTile() {
        SpecialProperty property = action.getSpecialProperty();
        if (property instanceof SpecialBaseTokenLay) {
            if (((SpecialBaseTokenLay)property).requiresTile()) {
                return hex.getHex().isPreprintedTileCurrent();
            }
        }
        return false;
    }

    public boolean requiresNoTile() {
        SpecialProperty property = action.getSpecialProperty();
        if (property instanceof SpecialBaseTokenLay) {
            if (((SpecialBaseTokenLay)property).requiresNoTile()) {
                return !hex.getHex().isPreprintedTileCurrent();
            }
        }
        return false;
    }

    // HexUpgrade abstract methods
    
    @Override
    public boolean hasSingleSelection() {
        return allowed.size() == 1;
    }

    @Override
    public void firstSelection() {
        selectedStop = allowed.first();
    }
    
    @Override
    public void nextSelection() {
        Stop next = allowed.higher(selectedStop);
        if (next == null) {
            selectedStop =  allowed.first();
        } else {
            selectedStop = next;
        }
    }

    @Override
    public Set<HexUpgrade.Invalids> getInvalids() {
        return ImmutableSet.<HexUpgrade.Invalids>copyOf(invalids);
    }
    
    @Override
    public boolean isValid() {
        return invalids.isEmpty();
    }
    
    @Override
    public int getCost() {
        return action.getPotentialCost(hex.getHex());
    }
    
    @Override
    public Image getUpgradeImage(int zoomStep) {
        Color fgColour = null;
        Color bgColour = null;
        String label = null;
        if (action instanceof LayBaseToken) {
            PublicCompany comp = ((LayBaseToken) action).getCompany();
            fgColour = comp.getFgColour();
            bgColour = comp.getBgColour();
            label = comp.getId();
        } else if (action instanceof LayBonusToken) {
            fgColour = Color.BLACK;
            bgColour = Color.WHITE;
            BonusToken token = ((LayBonusToken)action).getSpecialProperty().getToken();
            label = "+" + token.getValue();
        }
        
        TokenIcon icon = new TokenIcon(40, fgColour, bgColour, label);
        BufferedImage tokenImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        icon.paintIcon(new JLabel(), tokenImage.getGraphics(), 0, 0);
        
        return tokenImage;

    }
    
    @Override
    public String getUpgradeText() {
        String text = null;
        if (action instanceof LayBaseToken) {
            text = "<html>";
            if (action.getPotentialCost(hex.getHex()) != 0) {
                String cost = Bank.format(action.getCompany(), action.getPotentialCost(hex.getHex()));
                text += LocalText.getText("TOKEN_UPGRADE_COST", cost);
            } else {
                text += LocalText.getText("TOKEN_UPGRADE_FOR_FREE");
            }
            if (action.getSpecialProperty() != null) {
                text +=
                        "<br> <font color=red> ["
                                + action.getSpecialProperty().getOriginalCompany().getId()
                                + "] </font>";
            }
            if (isValid() && !hasSingleSelection()) {
                text += "<br> <font size=-2>";
                text += hex.getHex().getConnectionString(selectedStop.getRelatedStation());
                text += "</font>";
            }
            text += "</html>";
        } else if (action instanceof LayBonusToken) {
            BonusToken token = ((LayBonusToken)action).getSpecialProperty().getToken();
            text = token.getId();
        }
        return text;
    }
    
    @Override
    public String getUpgradeToolTip() {
        StringBuilder tt = new StringBuilder("<html>");
        if (!isValid()) {
            tt.append(invalidToolTip());
        } else {
            if (action instanceof LayBaseToken) {
                tt.append(LocalText.getText("TOKEN_UPGRADE_TT_VALID", action.getCompany()));
            }
        }
        tt.append("</html>");
        return tt.toString();
    }

    // FIXME: Move that to an Invalids class (indentical code in TileHexUpgrade)
    private String invalidToolTip() {
        StringBuilder tt = new StringBuilder();

        tt.append("<b><u>");
        tt.append(LocalText.getText("TOKEN_UPGRADE_TT_INVALID")); 
        tt.append("</u></b><br>");

        tt.append("<b>");
        for (Invalids invalid : invalids) {
            tt.append(invalid.toString() + "<br>");
        }
        tt.append("</b>");

        return tt.toString();
    }



    @Override
    public int getCompareId() {
        return 1;
    }
    
    /**
     * Sorting is based on the following: First special Tokens, then type id of action (see there),
     * followed by valuable stations, with less token slots left
     */
    @Override
    public Comparator<HexUpgrade> getComparator () {
        return new Comparator<HexUpgrade>() {
            
            @Override
            public int compare(HexUpgrade u1, HexUpgrade u2) {
                if (u1 instanceof TokenHexUpgrade && u2 instanceof TokenHexUpgrade) {
                    TokenHexUpgrade ts1 = (TokenHexUpgrade) u1;
                    TokenHexUpgrade ts2 = (TokenHexUpgrade) u2;

                    boolean base1 = ts1.action instanceof LayBaseToken;
                    boolean base2 = ts2.action instanceof LayBaseToken;

                    int type1 = 0, type2 = 0;
                    if (base1) {
                        type1 = ((LayBaseToken)ts1.action).getType();
                    }
                    if (base2) {
                        type2 = ((LayBaseToken)ts2.action).getType();
                    }
                    
                    PublicCompany company1 = ts1.action.getCompany();
                    PublicCompany company2 = ts2.action.getCompany();
                    
                    return ComparisonChain.start()
                            .compare(base1, base2)
                            .compare(type2, type1)
                            .compare(company1, company2)
                            .result();
                }
                return 0;
            }
        };
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("stops", stops)
                .add("action", action)
                .toString();
    }

    /**
     * sets both validation and visibility for upgrades
     */
    public static void validates(TokenHexUpgrade upgrade) {
        if (upgrade.validate()) {
            upgrade.setVisible(true);
        }
    }
}
