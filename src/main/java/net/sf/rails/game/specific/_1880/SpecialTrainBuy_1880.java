package net.sf.rails.game.specific._1880;

import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.util.Util;
import rails.game.*;

/**
 * Special private ability involving deductions in train buying. The deduction
 * can be absolute (an amount) or relative (a percentage)
 *
 * @author Erik Voss
 * @author Martin Brumm 
 * 
 */
public class SpecialTrainBuy_1880 extends SpecialProperty {

    protected SpecialTrainBuy_1880(RailsItem parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    String name = "SpecialTrainBuy_1880";
    String trainTypeName = ""; // Default: all train types
    List<TrainType> trainTypes =null;
    boolean extra = false;
    String deductionString;
    boolean relativeDeduction = false;
    boolean absoluteDeduction = false;
    int deductionAmount; // Money or percentage

    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        super.configureFromXML(tag);

        Tag trainBuyTag = tag.getChild("SpecialTrainBuy_1880");
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

    @Override
    public void finishConfiguration (RailsRoot root) 
    throws ConfigurationException {
        trainTypes=getRoot().getTrainManager().parseTrainTypes(trainTypeName);
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
        return "SpecialTrainBuy comp=" + originalCompany.getLongName() + " extra="
               + extra + " deduction=" + deductionString;
    }
    
    @Override
    public String toMenu() {
    
            return LocalText.getText("SpecialTrainBuy_1880",
                    trainTypeName,
                    deductionString,
                    originalCompany.getLongName());        
    }
    
    public String getInfo() {
        return toMenu();
    }
}
