package net.sf.rails.game.model;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

public class ParSlotModel extends RailsModel {

    private String name = null;
    private PublicCompany company = null;

    public ParSlotModel(RailsRoot parent,String name) {
        super(parent, name);
        this.name = name;
    }
    
    public void setCompany(PublicCompany company) {
        this.company = company;
        
    }
    
    public Object getUpdate() {
        if (company != null) {
            return new ViewUpdate(getText())
                .addObject(ViewUpdate.BGCOLOUR, company.getBgColour())
                .addObject(ViewUpdate.FGCOLOUR, company.getFgColour());
        } else {
            return getText();
        }
    }
    
    public boolean isEmpty() {
        return (company == null);
    }
    
    public String getText() {
        if (company != null) {
            return company.getId();
        }
        return " ";
    }

    public String toText() {
        if (company != null) {
            return company.getId();
        }
        return " ";
    }
    
    // From StateI
    public String getName() {
        if (company == null) {
            return "";
        } else {
            return company.getLongName();
        }
    }

    // From StateI
    public Object get() {
        return company;
    }

    // From StateI
    public void setState(Object value) {
        if (value == null) {
            company = null;
        } else if (value instanceof PublicCompany) {
            company = (PublicCompany) value;
        } else {
            new Exception("Incompatible object type "
                          + value.getClass().getName()
                          + "passed to ParSlotModel " + name).printStackTrace();
        }
        
    }

}
