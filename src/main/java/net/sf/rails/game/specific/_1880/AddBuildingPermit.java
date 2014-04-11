package net.sf.rails.game.specific._1880;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.special.SpecialProperty;

/**
 * Special private ability involving deductions in train buying. The deduction
 * can be absolute (an amount) or relative (a percentage)
 * 
 * @author Michael Alexander
 * 
 */
public class AddBuildingPermit extends SpecialProperty {
    String name = "AddBuildingPhasePermit";
    String addedPermitName = "D"; // Default: Phase D
    
    public AddBuildingPermit(RailsItem parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }



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

    public String getId() {
        return name;
    }

    public String getPermitName() {
        return addedPermitName;
    }

    public String toString() {
        return "Add \"" + addedPermitName + "\"building rights to company";
    }
    
    @Override
    public String toMenu() {
        return LocalText.getText("AddRights", addedPermitName); 
    }
}
