package rails.game.model;

import rails.game.PublicCompanyI;
import rails.game.state.StateI;

public class ParSlotModel extends ModelObject implements StateI {

    private String name = null;
    private PublicCompanyI company = null;

    public ParSlotModel(String name) {
        this.name = name;
    }
    
    public void setCompany(PublicCompanyI company) {
        this.company = company;
        update();
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
    
    // From ModelObject
    @Override
    public String getText() {
        if (company != null) {
            return company.getName();
        }
        return " ";
    }

    // From StateI
    public String getName() {
        if (company == null) {
            return "";
        } else {
            return company.getName();
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
            update();
        } else if (value instanceof PublicCompanyI) {
            company = (PublicCompanyI) value;
            update();
        } else {
            new Exception("Incompatible object type "
                          + value.getClass().getName()
                          + "passed to ParSlotModel " + name).printStackTrace();
        }
        
    }

}
