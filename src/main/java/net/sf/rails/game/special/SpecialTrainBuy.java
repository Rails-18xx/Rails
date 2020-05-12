package net.sf.rails.game.special;

import org.apache.commons.lang3.StringUtils;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsItem;
import net.sf.rails.util.Util;

/**
 * Special private ability involving deductions in train buying. The deduction
 * can be absolute (an amount) or relative (a percentage)
 */
public class SpecialTrainBuy extends SpecialProperty {

    protected String name = "SpecialTrainBuy";
    protected String trainTypeName = ""; // Default: all train types
    protected boolean extra = false;
    protected String deductionString;
    protected boolean relativeDeduction = false;
    protected boolean absoluteDeduction = false;
    protected int deductionAmount; // Money or percentage

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialTrainBuy(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag trainBuyTag = tag.getChild("SpecialTrainBuy");
        if (trainBuyTag == null) {
            throw new ConfigurationException("<SpecialTrainBuy> tag missing");
        }

        trainTypeName = trainBuyTag.getAttributeAsString("trainType", trainTypeName);
        if ( "any".equalsIgnoreCase(trainTypeName)) trainTypeName = "";

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
        return StringUtils.isBlank(trainTypeName) || trainTypeName.equalsIgnoreCase(trainType);
    }

    @Override
    public boolean isExecutionable() {
        return true;
    }

    public boolean isExtra() {
        return extra;
    }

    public boolean isFree() {
        return false;
    }

    @Override
    public String getId() {
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

    @Override
    public String toText() {
        return "SpecialTrainBuy comp=" + originalCompany.getId() + " extra=" + extra + " deduction=" + deductionString;
    }

    @Override
    public String toMenu() {
        if ( StringUtils.isBlank(trainTypeName)) {
            return LocalText.getText("SpecialTrainBuyAny", deductionString, originalCompany.getId());
        }
        else {
            return LocalText.getText("SpecialTrainBuy", trainTypeName, deductionString, originalCompany.getId());
        }
    }

    @Override
    public String getInfo() {
        return toMenu();
    }
}
