/**
 * 
 */
package rails.game.specific._1880;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.PhaseI;
import rails.game.Player;
import rails.game.special.SpecialProperty;

/**
 * @author Martin
 *
 */
public class ExchangeForCash_1880 extends SpecialProperty {

    int[] phaseAmount = {40,70,100};
    
    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag swapTag = tag.getChild("ExchangeForCash");
        if (swapTag == null) {
            throw new ConfigurationException("<ExchangeForCash> tag missing");
        }

//        String amount = swapTag.getAttributeAsString("amount");
//        if (!Util.hasValue(amount))
//            throw new ConfigurationException(
//                    "ExchangeForCash: amounts are missing");
        
    }
    
    /* (non-Javadoc)
     * @see rails.game.special.SpecialPropertyI#isExecutionable()
     */
    public boolean isExecutionable() {
     
        return originalCompany.getPortfolio().getOwner() instanceof Player;
    }

    /* (non-Javadoc)
     * @see rails.game.special.SpecialPropertyI#getName()
     */
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "Swap " + originalCompany.getName() + " for " + "{40, 70, 100 }"
               + "Yuan";
    }

    public int getPhaseAmount() {
        String currentPhase;
        
    currentPhase = gameManager.getCurrentPhase().getName();
    if (currentPhase == "3") {
        return phaseAmount[0];
    } else if  (currentPhase == "3+3"){
        return phaseAmount[1];
    } else if (currentPhase == "4") {
        return phaseAmount[2];
    }else {
           return 0;
       }
        
    }

    @Override
    public String toMenu() {
        return LocalText.getText("SwapPrivateForMoney",
                originalCompany.getName(),
                "40, 70, 100 Yuan depending on the active Train");
    }
    
    public String getInfo() {
        return toMenu();
    }
}
