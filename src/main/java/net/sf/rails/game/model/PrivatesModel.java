package net.sf.rails.game.model;

import net.sf.rails.game.Company;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.game.state.PortfolioSet;

public final class PrivatesModel extends RailsModel {

    public static final String ID = "PrivatesModel";
    
    private final Portfolio<PrivateCompany> privates;
    
    private boolean addLineBreak = false;

    private PrivatesModel(RailsOwner parent, String id) {
        super(parent, id);
        privates = PortfolioSet.create(parent, "privates", PrivateCompany.class);
    }
    
    /**
     * Creates an initialized PrivatesModel
     */
    public static PrivatesModel create(RailsOwner parent) {
        return new PrivatesModel(parent, ID);
    }

    @Override
    public RailsOwner getParent() {
        return (RailsOwner)super.getParent();
    }
    
    public Portfolio<PrivateCompany> getPortfolio() {
        return privates;
    }
    
    public void moveInto(PrivateCompany p){
        privates.moveInto(p);
    }
    
    
    public void setLineBreak(boolean lineBreak) {
        this.addLineBreak = lineBreak;
    }

    @Override
    public String toText() {

        StringBuffer buf = new StringBuffer("<html>");
        for (Company priv : privates) {
            if (buf.length() > 6)
                buf.append(addLineBreak ? "<br>" : "&nbsp;");
            buf.append(priv.getId());
        }
        if (buf.length() > 6) {
            buf.append("</html>");
        }
        return buf.toString();

    }

}
