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
        // PrivatesModel is an indirect owner of privates, so add it to the state
        privates.addModel(this);
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
    
    public float getCertificateCount() {
        float count = 0;
        for (PrivateCompany p:privates) {
            count += p.getCertificateCount();
        }
        return count;
    }
    
    public void moveInto(PrivateCompany p){
        privates.add(p);
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
