package rails.game.specific._1880;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.special.SpecialProperty;

/**
 * Special private ability involving deductions in train buying. The deduction
 * can be absolute (an amount) or relative (a percentage)
 * 
 * @author Erik Vos
 * 
 */
public class AddBuildingPermit extends SpecialProperty {

    String name = "SpecialTrainBuy";
    String addedPermitName = ""; // Default: all train types

    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        super.configureFromXML(tag);

        Tag addedPermitTag = tag.getChild("AddedPermit");
        if (addedPermitTag == null) {
            throw new ConfigurationException("<AddedPermit> tag missing");
        }

        addedPermitName = addedPermitTag.getAttributeAsString("name", addedPermitName);
    }

    public boolean isExecutionable() {
        return true;
    }

    public String getName() {
        return name;
    }

    public String getPermitName() {
        return addedPermitName;
    }

    public String toString() {
        return "Add \"" + addedPermitName + "\"building rights to comp=" + originalCompany.getName();
    }
    
    @Override
    public String toMenu() {
        return LocalText.getText("AddRights", addedPermitName); 
    }
}
