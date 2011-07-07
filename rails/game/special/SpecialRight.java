/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialRight.java,v 1.19 2010/05/05 21:37:18 evos Exp $ */
package rails.game.special;

import java.util.Set;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.util.*;

public class SpecialRight extends SpecialProperty implements RevenueStaticModifier {

    /** The public company of which a share can be obtained. */
    protected String rightName;
    protected String rightDefaultValue;
    protected String rightValue;
    protected int cost;

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag rightTag = tag.getChild("SpecialRight");
        if (rightTag == null) {
            throw new ConfigurationException("<SpecialRight> tag missing");
        }

        rightName = rightTag.getAttributeAsString("name");
        if (!Util.hasValue(rightName))
            throw new ConfigurationException(
                    "SpecialRight: no Right name specified");
        
        rightDefaultValue = rightValue = rightTag.getAttributeAsString("defaultValue", null);

        cost = rightTag.getAttributeAsInteger("cost", 0);
    }

    @Override
    public void finishConfiguration (GameManagerI gameManager) throws ConfigurationException {
        super.finishConfiguration(gameManager);
        
        // add them to the call list of the RevenueManager
        gameManager.getRevenueManager().addStaticModifier(this);
    }
    
    public boolean isExecutionable() {

        return originalCompany.getPortfolio().getOwner() instanceof Player;
    }

 
    public String getName() {
        return rightName;
    }

    public String getDefaultValue() {
        return rightDefaultValue;
    }

    public String getValue() {
        return rightValue;
    }

    public void setValue(String rightValue) {
        this.rightValue = rightValue;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Buy '" + rightName + "' right for " + Bank.format(cost);
    }
    
    @Override
    public String toMenu() {
        return LocalText.getText("BuyRight",
                rightName,
                Bank.format(cost));
    }
    
    public String getInfo() {
        return toMenu();
    }

    /** 
     *  modify revenue calculation of the 
     *  TODO: rights is missing a location field, currently hardcoded for 1830 coalfields 
     */
    public void modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. check operating company if it has the right then it is excluded from the removal
        if (revenueAdapter.getCompany().hasRight(rightName)) return;
        
        // 2. find vertices to hex and remove those
        MapHex hex = GameManager.getInstance().getMapManager().getHex("L10");
        Set<NetworkVertex> verticesToRemove = NetworkVertex.getVerticesByHex(revenueAdapter.getVertices(), hex);
        revenueAdapter.getGraph().removeAllVertices(verticesToRemove);
    }
}
