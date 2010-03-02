/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTrainBuy.java,v 1.5 2010/03/02 21:14:16 evos Exp $ */
package rails.game.special;

import rails.game.*;
import rails.util.LocalText;
import rails.util.Tag;
import rails.util.Util;

/**
 * Special private ability involving deductions in train buying. The deduction
 * can be absolute (an amount) or relative (a percentage)
 * 
 * @author Erik Vos
 * 
 */
public class SpecialTrainBuy extends SpecialProperty {

    String name = "SpecialTrainBuy";
    String trainTypeName = ""; // Default: all train types
    boolean extra = false;
    String deductionString;
    boolean relativeDeduction = false;
    boolean absoluteDeduction = false;
    int deductionAmount; // Money or percentage

    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        super.configureFromXML(tag);

        Tag trainBuyTag = tag.getChild("SpecialTrainBuy");
        if (trainBuyTag == null) {
            throw new ConfigurationException("<SpecialTrainBuy> tag missing");
        }

        trainTypeName =
                trainBuyTag.getAttributeAsString("trainType", trainTypeName);
        if (trainTypeName.equalsIgnoreCase("any")) trainTypeName = "";

        deductionString = trainBuyTag.getAttributeAsString("deduction");
        if (!Util.hasValue(deductionString)) {
            throw new ConfigurationException(
                    "No deduction found in <SpecialTrainBuy> tag");
        }
        String deductionAmountString;
        if (deductionString.endsWith("%")) {
            relativeDeduction = true;
            deductionAmountString = deductionString.replaceAll("%", "");
        } else {
            deductionAmountString = deductionString;
        }
        try {
            deductionAmount = Integer.parseInt(deductionAmountString);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid deduction "
                                             + deductionString, e);
        }

    }

    public int getPrice(int standardPrice) {

        if (absoluteDeduction) {
            return standardPrice - deductionAmount;
        } else if (relativeDeduction) {
            return (int) (standardPrice * (0.01 * (100 - deductionAmount)));
        } else {
            return standardPrice;
        }
    }

    public boolean isValidForTrainType(String trainType) {
        return trainTypeName.equals("")
               || trainTypeName.equalsIgnoreCase(trainType);
    }

    public boolean isExecutionable() {
        return true;
    }

    public boolean isExtra() {
        return extra;
    }

    public boolean isFree() {
        return false;
    }

    public String getName() {
        return name;
    }

    public boolean isAbsoluteDeduction() {
        return absoluteDeduction;
    }

    public int getDeductionAmount() {
        return deductionAmount;
    }

    public String getDeductionString() {
        return deductionString;
    }

    public boolean isRelativeDeduction() {
        return relativeDeduction;
    }

    public String getTrainTypeName() {
        return trainTypeName;
    }

    public String toString() {
        return "SpecialTrainBuy comp=" + originalCompany.getName() + " extra="
               + extra + " deduction=" + deductionString;
    }
    
    @Override
    public String toMenu() {
        if (trainTypeName.equals("")) {
            return LocalText.getText("SpecialTrainBuyAny",
                    deductionString,
                    originalCompany.getName());
        }
        else {
            return LocalText.getText("SpecialTrainBuy",
                    trainTypeName,
                    deductionString,
                    originalCompany.getName());
        }        
    }
    
    public String getInfo() {
        return toMenu();
    }
}
